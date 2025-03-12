package com.magicvector.ai.wizards.impl;

import com.github.tbwork.anole.loader.util.JSON;
import com.github.tbwork.anole.loader.util.S;
import com.google.gson.JsonObject;
import com.magicvector.ai.brain.Brain;
import com.magicvector.ai.brain.llm.openai.model.GPTResponse;
import com.magicvector.ai.core.manager.SpellManager;
import com.magicvector.ai.exceptions.AIBusyException;
import com.magicvector.ai.exceptions.MessageStreamException;
import com.magicvector.ai.model.Role;
import com.magicvector.ai.model.WizardStatus;
import com.magicvector.ai.util.IOUtil;
import com.magicvector.ai.util.SpellUtil;
import com.magicvector.ai.wizards.IChatWizard;
import com.magicvector.ai.wizards.model.MagicChat;
import com.magicvector.ai.wizards.model.MagicMessage;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * Core.
 */
public class ChatWizard implements IChatWizard {

    private static final Logger logger = LoggerFactory.getLogger(ChatWizard.class);

    private static final String spellQuote = "@#%";

    private static final String spellQuoteFirstChar = spellQuote.charAt(0) +"";

    private Brain brain;

    /**
     * 回答发生错误时容忍的上限
     */
    private Integer maxRounds;


    private static final String PARSE_ERROR = "解析AI流式返回时出错，请联系系统管理员。";


    public ChatWizard(Brain brain, Integer maxRounds){
        this.brain = brain;
        this.maxRounds = maxRounds;
    }

    public ChatWizard(Brain brain){
        this.brain = brain;
        this.maxRounds = 20;
    }


    @Override
    public void executeSpells(MagicChat magicChat, List<String> spellTexts){
        if(spellTexts.isEmpty()){
            return;
        }

        WizardStatus statusStore = magicChat.getWizardStatus();
        // 进入念咒语的阶段
        magicChat.setStatus(WizardStatus.SPELLING);

        int p =1;
        StringBuilder sb = new StringBuilder();
        for(String spellText:spellTexts){
            sb.append("["+ p++ +"] ");
            logger.debug("AI 咒语：{}", spellText.length() > 50 ? spellText.substring(0, 50) + "..." : spellText);
            String spellResult = SpellManager.execSpell(spellText);
            sb.append(spellResult).append("\n");
        }

        String spellResultText =  "#S# "  +  sb.toString() +  " #E#";
        logger.debug("{}:{}", "咒语执行成功, 返回为", spellResultText);

        magicChat.appendMessage(new MagicMessage("system", spellResultText));

        logger.debug(JSON.toJSONString(magicChat.getChatContent()));
        //  恢复进来时的状态。
        magicChat.setStatus(statusStore);

    }



    @Override
    public String doThink(MagicChat chat, OutputStream outputStream) {
        // 如果AI在忙，报错
        if(!chat.isIdle()){
            throw new AIBusyException(chat.getWizardStatus());
        }

        String aiResponse = "";
        int p = 0;
        while( p++ < maxRounds){
            try {
                // 大脑的输出就是这里的输入流
                InputStream inputStream = brain.process(chat);
                chat.setStatus(WizardStatus.RESPONDING);
                aiResponse = processMixedResponseStream(inputStream, outputStream);
                logger.debug(aiResponse);
                chat.appendMessage(Role.ASSISTANT, aiResponse);
                /*
                  如果输出为咒语：
                  1. 执行咒语(获得结果并放入Chat中)
                  2. 重新调用generate获取最新结果, outputstream不变
                 */
                List<String> spells = SpellUtil.findSpells(aiResponse);
                if(spells.isEmpty()){
                    //没有咒语了，就说AI已经完成了任务。
                    chat.setStatus(WizardStatus.IDLE);
                    outputStream.close();
                    break;
                }
                executeSpells(chat, spells);
            } catch (Exception e) {
                throw new MessageStreamException("AI回答加工处理失败，原因：" + e.getMessage());
            }
        }
        return aiResponse;

    }

    @Override
    public String doResponse(MagicChat chat) {
        // 如果AI在忙，报错
        if(!chat.isIdle()){
            throw new AIBusyException(chat.getWizardStatus());
        }
        String responseText = brain.response(chat);
        GPTResponse gptResponse = JSON.parseObject(responseText, GPTResponse.class);

        Asserts.check( gptResponse.getChoices()!=null
                && !gptResponse.getChoices().isEmpty()
                && gptResponse.getChoices().get(0).getMessage()  != null
                && S.isNotEmpty(gptResponse.getChoices().get(0).getMessage().getContent()),
                "LLM返回的数据格式不正确。"
        );

        return gptResponse.getChoices().get(0).getMessage().getContent();
    }


    /**
     * 处理AI的输出流（对这里来说是输入流），正常的可见输出直接放入输出流。
     * @param aiResponseStream AI大脑输出的流,可能存在混排，也就是正常内容+咒语。
     * @param outputStream 指定的输出流
     * @return 返回整个AI的返回（包括内容+咒语）
     * @throws IOException
     */
    private String processMixedResponseStream(InputStream aiResponseStream, OutputStream outputStream) throws IOException {

        StringBuilder responseBuffer = new StringBuilder();
        StringBuilder spellStartTag = new StringBuilder();
        StringBuilder spellEndTag = new StringBuilder();

        if(aiResponseStream != null){
            // 仅当当前的脑处理器有内容输出才需要处理，没有输出也是可以的。
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(aiResponseStream));
            String line;//每一行都是一个返回的片段。
            while ((line = bufferedReader.readLine()) != null) {
                if(line.isEmpty()){
                    continue;
                }
                if(this.isErrorResponse(line)){
                    this.readAndLogError(bufferedReader);
                    IOUtil.writeToOutputStream(PARSE_ERROR, outputStream);
                    break;
                }
                String chunkText = brain.parseChunk(line);
                if("EOF".equals(chunkText)){
                    //结束了
                    aiResponseStream.close();
                    break;
                }
                responseBuffer.append(chunkText);
                int p = 0;
                while( p < chunkText.length()){
                    String charText = chunkText.substring(p,p+1);
                    p++;
                    if(spellStartTag.length()<spellQuote.length()){
                        spellStartTag.append(charText);
                        if(!spellQuote.startsWith(spellStartTag.toString())){
                            IOUtil.writeToOutputStream(charText, outputStream);
                            spellStartTag = new StringBuilder();
                            continue;
                        }
                    }

                    if(spellQuote.contentEquals(spellStartTag)){
                        if(spellEndTag.length()<spellQuote.length()){
                            spellEndTag.append(charText);
                            if(!spellQuote.startsWith(spellEndTag.toString())){
                                spellEndTag = new StringBuilder();
                            }
                        }
                        if(spellQuote.contentEquals(spellEndTag)){
                            // 重置
                            spellStartTag = new StringBuilder();
                            spellEndTag = new StringBuilder();
                        }
                    }

                }
            }
        }
        return responseBuffer.toString();
    }



    /**
     * 处理AI的输出流（对这里来说是输入流），正常的可见输出直接放入输出流。
     * @param aiResponseStream AI大脑输出的流
     * @param outputStream 指定的输出流
     * @return 如果是咒语，那就是咒语文本；否则返回AI的回答内容。
     * @throws IOException
     */
    private String processResponseStream(InputStream aiResponseStream, OutputStream outputStream) throws IOException {
        StringBuilder spellBuffer = new StringBuilder();
        StringBuilder responseBuffer = new StringBuilder();

        if(aiResponseStream != null){
            // 仅当当前的脑处理器有内容输出才需要处理，没有输出也是可以的。
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(aiResponseStream));
            String line;//每一行都是一个返回的片段。
            while ((line = bufferedReader.readLine()) != null) {
                if(line.isEmpty()){
                    continue;
                }
                if(this.isErrorResponse(line)){
                    this.readAndLogError(bufferedReader);
                    IOUtil.writeToOutputStream(PARSE_ERROR, outputStream);
                    break;
                }
                String chunkText = brain.parseChunk(line);
                if("EOF".equals(chunkText)){
                    //结束了
                    aiResponseStream.close();
                    break;
                }
                responseBuffer.append(chunkText);
                if(chunkText.startsWith(spellQuoteFirstChar) || !spellBuffer.toString().isEmpty()){
                    // 采集前几个字符确认是否为咒语
                    spellBuffer.append(chunkText);
                    // 咒语采集和处理
                    if(spellBuffer.length() >= spellQuote.length() && !spellBuffer.toString().startsWith(spellQuote)){
                        // 代表不是咒语，直接放过。
                        IOUtil.writeToOutputStream(spellBuffer.toString(), outputStream);
                        spellBuffer = new StringBuilder();
                    }
                }
                else if(!chunkText.isEmpty()){
                    IOUtil.writeToOutputStream(chunkText, outputStream);
                }
            }

        }
        return responseBuffer.toString();
    }

    private boolean isErrorResponse(String firstLine){
        return "{".equals(firstLine.trim());
    }


    private void readAndLogError(BufferedReader bufferedReader){
        try{
            StringBuilder responseTextBuilder = new StringBuilder();
            responseTextBuilder.append("{");
            String line;
            while ((line = bufferedReader.readLine()) != null){
                responseTextBuilder.append(line);
            }

            String error = "未知错误";
            JsonObject errorObj = (JsonObject) JSON.parse(responseTextBuilder.toString());
            if(errorObj.has("error") && (errorObj.getAsJsonObject("error").has("code") || errorObj.getAsJsonObject("error").has("message"))){

                String code = (errorObj.getAsJsonObject("error").get("code")==null ? "" : errorObj.getAsJsonObject("error").get("code")).toString();
                String message = errorObj.getAsJsonObject("error").get("message")==null?"":errorObj.getAsJsonObject("error").get("message").toString();
                logger.error("Failed to call OpenAI API. Details: {}:{}", code, message);
            }
        }
        catch (Exception e){
            logger.error("Fails to read gpt api's error message. Details: {}", e);
        }

    }



}
