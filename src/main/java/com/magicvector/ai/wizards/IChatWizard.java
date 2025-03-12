package com.magicvector.ai.wizards;

import com.magicvector.ai.prompts.IPrompt;
import com.magicvector.ai.wizards.model.MagicChat;
import java.io.OutputStream;
import java.util.List;

public interface IChatWizard {

    /**
     * 根据已有的对话，生成AI回答，返回到一个输出流中。
     */
    String doThink(MagicChat chat, OutputStream outputStream);

    String doResponse(MagicChat chat);


    void executeSpells(MagicChat chat, List<String> spellTexts);


}
