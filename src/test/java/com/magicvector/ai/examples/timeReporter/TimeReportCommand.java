package com.magicvector.ai.examples.timeReporter;

import com.magicvector.ai.annotation.MagicBook;
import com.magicvector.ai.annotation.SpellDefinition;
import com.magicvector.ai.util.DateUtil;

@MagicBook(name="timeReporter")
public class TimeReportCommand {

    @SpellDefinition(name = "queryTime", description= "查询当前的时间")
    public static String queryTime() {
        return DateUtil.getCurrentTime();
    }

}
