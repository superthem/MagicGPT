package com.magicvector.ai.brain;
import com.magicvector.ai.wizards.model.MagicChat;
import java.io.InputStream;

/**
 * 大脑：思考处理器
 */
public interface Brain {


    /**
     * 流式处理
     * 根据上下文进行思考，不断的将结果输出到指定流对象中
     *
     * @param magicChat
     * @return
     */
    public InputStream process(MagicChat magicChat);


    /**
     *
     * 从流式片段中解析出内容片段。
     * 此方法往往对于远端脑比较重要。
     * 如果是可以直接输出内容片段的本地脑，输入即返回。
     * @param chunk 流式片段
     * @return 内容片段
     */
    public String parseChunk(String chunk);


    /**
     * 非流式处理，直接响应
     * @param magicChat
     * @return
     */
    public String response(MagicChat magicChat);



}
