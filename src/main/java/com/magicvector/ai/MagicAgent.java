package com.magicvector.ai;

import com.magicvector.ai.brain.Brain;
import com.magicvector.ai.brain.llm.openai.GeneralBrain;
import com.magicvector.ai.wizards.IChatWizard;
import com.magicvector.ai.wizards.impl.ChatWizard;
import com.magicvector.ai.wizards.model.MagicChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.OutputStream;

import static com.magicvector.ai.util.PromptUtil.compileSpellPrompt;

public class MagicAgent {

    private static final Logger logger = LoggerFactory.getLogger(MagicAgent.class);

    private IChatWizard chatWizard;

    private MagicChat magicChat;


    public MagicAgent(Brain brain, String systemPrompt){
        this(brain, systemPrompt, 20);
    }

    public MagicAgent(String generalBrainModelName, String systemPrompt){
        this(new GeneralBrain(generalBrainModelName), systemPrompt, 20);
    }

    /**
     * 创建一个MagicGPT帮助类，自动搜索并注册指定包名下的所有Call类型咒语
     * @param brain AI魔法师的大脑
     * @param systemPrompt 系统提示词
     * @param maxRounds 使用咒语的最大轮数（防止陷入无限调用）
     */
    public MagicAgent(Brain brain, String systemPrompt, int maxRounds){
        this.chatWizard = new ChatWizard(brain, maxRounds);
        this.magicChat = startChat(systemPrompt);
    }

    private MagicChat startChat(String systemPrompt){
        String compiledSystemPrompt = compileSpellPrompt(systemPrompt);
        logger.info("系统提示词如下: \n{}", compiledSystemPrompt);
        // 传入system提示词
        MagicChat magicChat = new MagicChat();
        magicChat.appendSystemMessage(compiledSystemPrompt);
        return magicChat;
    }

    public void clearContext(){
        this.magicChat.clearConversation();
    }
    public String proceedChatWithStream(OutputStream outputStream){
        return chatWizard.doThink(this.magicChat, outputStream);
    }

    public boolean isIdle(){
        return this.magicChat.isIdle();
    }

    public String proceedWithStream(String userMessage, OutputStream outputStream){
        this.magicChat.appendUserMessage(userMessage);
        return chatWizard.doThink(this.magicChat, outputStream);
    }

    public String proceed(String userMessage){
        this.magicChat.appendUserMessage(userMessage);
        return chatWizard.doResponse(this.magicChat);
    }

}
