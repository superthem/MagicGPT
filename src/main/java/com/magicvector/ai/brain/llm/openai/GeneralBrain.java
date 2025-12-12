package com.magicvector.ai.brain.llm.openai;

import com.github.tbwork.anole.loader.util.S;
import com.magicvector.ai.brain.llm.openai.model.*;
import com.magicvector.ai.exceptions.Assert;
import com.magicvector.ai.exceptions.MagicGPTGeneralException;
import com.magicvector.ai.exceptions.MessageStreamException;
import com.magicvector.ai.exceptions.RemoteLLMCallException;
import com.magicvector.ai.wizards.model.MagicChat;
import com.magicvector.ai.wizards.model.MagicMessage;
import com.github.tbwork.anole.loader.Anole;
import com.github.tbwork.anole.loader.util.JSON;
import com.magicvector.ai.brain.llm.AbstractRemoteBrain;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GeneralBrain extends AbstractRemoteBrain {

    private static final Logger logger = LoggerFactory.getLogger(GeneralBrain.class);

    private String chatApiUrl;
    private String modelName;

    public GeneralBrain(String modelName) {
        super(
                Anole.getLongProperty("llm.api.timeout.read", 30L),
                Anole.getLongProperty("llm.api.timeout.connect", 10L),
                Anole.getLongProperty("llm.api.timeout.call", 1000L)
        );
        String llmApiUrl = Anole.getProperty("llm.api.chat.url");
        if(S.isEmpty(llmApiUrl)){
            llmApiUrl = Anole.getProperty("API_URL");
        }
        Assert.judge(S.isNotEmpty(llmApiUrl), "未提供有效的LLM API地址");
        this.chatApiUrl = llmApiUrl;
        this.modelName = modelName;
    }


    @Override
    public String parseChunk(String chunk) {
        logger.debug("ChunkData : {}", chunk);
        if(!chunk.startsWith("data:")){
            throw new MessageStreamException("Fail to parse LLM API's steam response. Chunk data:"+chunk);
        }

        chunk = chunk.replace("data:", "").trim();
        if(chunk.equals("[DONE]")){
            return "EOF";
        }

        try{
            GPTStreamData streamData = JSON.parseObject(chunk, GPTStreamData.class);
            StringBuilder sb = new StringBuilder();
            if(!streamData.getChoices().isEmpty()){
                for (GPTStreamDataChoice choice : streamData.getChoices()) {
                    if(S.isNotEmpty(choice.getDelta().getContent())){
                        sb.append(choice.getDelta().getContent());
                    }
                }
            }
            return sb.toString();
        }
        catch (Exception e){
            logger.error("Fail to parse LLM API's steam response. Chunk data: {} ", chunk );
            throw new MessageStreamException("Fail to parse LLM API's steam response. Chunk data:" + chunk );
        }
    }

    @Override
    public String response(MagicChat magicChat) {
        Response response = this.getOpenAIResponse(magicChat, false);
        // 如果响应成功，获取返回内容
        if (response.isSuccessful()) {
            try {
                // 获取响应体的字符串内容
                assert response.body() != null;
                return response.body().string();
            } catch (Exception e) {
                logger.error("Fail to call LLM, details: {}", e.getMessage(), e);
                throw new MagicGPTGeneralException("Fail to call LLM, details: " + e.getMessage());
            }
        } else {
            throw new MagicGPTGeneralException("Request failed with code: " + response.code());
        }
    }

    private String getResponseContent(String json){
        GPTResponse responseJson = JSON.parseObject(json, GPTResponse.class);
        if(!responseJson.getChoices().isEmpty()){
            for (GPTChoice choice : responseJson.getChoices()) {
                if(choice.getMessage()!=null && S.isNotEmpty(choice.getMessage().getContent())){
                    return choice.getMessage().getContent();
                }
            }
        }
        return "";
    }


    @Override
    public InputStream process(MagicChat magicChat) {
        Response response = this.getOpenAIResponse(magicChat, true);
        return response.body().byteStream();
    }



    private Response getOpenAIResponse(MagicChat magicChat, Boolean stream){
        try {

            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            //读取超时
            builder.readTimeout(this.readTimeoutSeconds, TimeUnit.SECONDS);
            //连接超时
            builder.connectTimeout(this.connectTimeoutSeconds, TimeUnit.SECONDS);
            //总超时时间
            builder.callTimeout(this.callTimeoutSeconds, TimeUnit.SECONDS);

            if(Anole.getBoolProperty("magicgpt.config.network.vpn.enabled", false)){
                Proxy vpn = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(Anole.getProperty("magicgpt.config.network.vpn.host"), Anole.getIntProperty("magicgpt.config.network.vpn.port")));
                builder.proxy(vpn);
            }

            OkHttpClient httpClient = builder.build();
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
            GPTRequest gptRequest = buildChatGPTRequest(magicChat, stream);
            logger.debug(" LLM Request：\n{}",  JSON.toJSONString(gptRequest));
            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, JSON.toJSONString(gptRequest));

            String apiKey = Anole.getProperty("AI_API_KEY");
            if(S.isEmpty(apiKey)){
                apiKey = Anole.getProperty("OPENAI_API_KEY");
            }
            Assert.judge(S.isNotEmpty(apiKey), "AI_API_KEY is not set yet.");
            // Create the request
            Request request = new Request.Builder()
                    .url(this.chatApiUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer "+ apiKey)
                    .post(requestBody)
                    .build();

            // Send the request and get the response
            return httpClient.newCall(request).execute();

        } catch (Exception e) {
            logger.error("Fails to call LLM API. Details: {}", e);
            throw new RemoteLLMCallException(e.getMessage());
        }
    }

    /**
     * 构建chatGPT 请求参数
     * @param magicChat
     * @return
     */
    private GPTRequest buildChatGPTRequest(MagicChat magicChat, Boolean stream) {
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(modelName);
        gptRequest.setMaxCompletionTokens(Anole.getIntProperty("llm.chat.response.max.length", 4096));
        gptRequest.setTemperature(Anole.getDoubleProperty("llm.chat.temperature", 0.6));
        gptRequest.setStream(stream);
        List<GPTMessage> messages = new ArrayList<>();
        for (MagicMessage magicMessage : magicChat.getChatContent()) {
            GPTMessage GPTMessage = new GPTMessage(magicMessage.getRole(), magicMessage.getContent());
            messages.add(GPTMessage);
        }
        gptRequest.setMessages(messages);
        return gptRequest;
    }


}
