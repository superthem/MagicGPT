package com.magicvector.ai.core.manager;

import com.magicvector.ai.exceptions.Assert;
import com.magicvector.ai.executors.IExecutor;
import com.magicvector.ai.executors.ITask;
import com.magicvector.ai.executors.impl.Task;
import com.magicvector.ai.executors.impl.Executor;
import com.magicvector.ai.core.Spell;
import com.magicvector.ai.prompts.impl.SpellPrompt;
import com.magicvector.ai.util.SpellUtil;
import org.apache.http.util.Asserts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 咒语管理器，包含基础的功能以及咒语执行管理
 */
public class SpellManager {

    private static final IExecutor executor = Executor.INSTANCE;

    private static final Map<String, Map<String,Spell>> magicSpellBooks = new HashMap<String, Map<String,Spell>>();

    private static final Map<String, Spell> quickMap = new HashMap<String, Spell>();


    public static String getSpellPrompt(String spellName){
        Spell spell = quickMap.get(spellName);
        Asserts.check(spell != null, "找不到对应名称（"+spellName+"）的咒语！");
        SpellPrompt spellPrompt = new SpellPrompt(spell);
        return spellPrompt.getPrompt();
    }

    public static String getSpellBookPrompt(String bookName){
        Map<String, Spell> spellBookMap = magicSpellBooks.get(bookName);
        Asserts.check(spellBookMap != null, "找不到对应名称（"+bookName+"）的咒语书！");
        StringBuilder result = new StringBuilder();
        for (Spell spell : spellBookMap.values()) {
            SpellPrompt spellPrompt = new SpellPrompt(spell);
            result.append(spellPrompt.getPrompt()).append("\n");
        }
        return result.toString();
    }

    public static void registerSpell(String bookName, Spell spell) {
        magicSpellBooks.computeIfAbsent(bookName, k -> new HashMap<String, Spell>());
        magicSpellBooks.get(bookName).put(spell.getApiName(), spell);
        Asserts.check(!quickMap.containsKey(spell.getApiName()), "存在重复的咒语，咒语名称："+spell.getApiName());
        quickMap.put(spell.getApiName(), spell);
    }

    public static List<Spell> getSpellsInBook(String bookName) {
        Map<String, Spell> spellMap = magicSpellBooks.get(bookName);
        if(spellMap != null){
            return new ArrayList<>(spellMap.values());
        }
        return new ArrayList<>();
    }

    public static List<Spell> getAllSpells() {
        List<Spell> result = new ArrayList<>();
        for (String bookName : magicSpellBooks.keySet()) {
            result.addAll(getSpellsInBook(bookName));
        }
        return result;
    }


    public static String execSpell(String spell){
        List<String> spellParts = SpellUtil.getSpellParts(spell);
        return doExecSpell(spellParts);
    }


    private static String doExecSpell(List<String> spellArgs) {
        try{
            Assert.isNotEmpty(spellArgs,"咒语参数列表不为空");
            String callName = spellArgs.get(0);
            Assert.isNotBlank(callName,"咒语名称不为空");
            // 1. 根据callName找到对应的咒语对象。
            Assert.judge(quickMap.get(callName) != null, "找不到名为 "+ callName +" 的咒语。");
            Spell callSpell = quickMap.get(callName);

            // 2. 构建咒语执行任务
            spellArgs.remove(0);
            ITask callTask = new Task(callSpell, spellArgs);

            // 3. 执行咒语返回文本结果
            return executor.execute(callTask);
        }
        catch (Exception e){
            return "ERROR:"+e.getMessage();
        }
    }

}
