package com.magicvector.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpellUtil {

    public static List<String> getSpellParts(String commandString) {
        List<String> result = new ArrayList<>();

        // 找到第一个空格，分离命令和参数部分
        int firstSpaceIndex = commandString.indexOf(' ');
        if (firstSpaceIndex == -1) {
            // 如果没有空格，整个字符串就是命令
            result.add(commandString);
            return result;
        }

        // 第一个部分是命令
        String command = commandString.substring(0, firstSpaceIndex);
        result.add(command);

        // 第二部分是参数部分
        String argsPart = commandString.substring(firstSpaceIndex + 1).trim();

        // 解析参数部分，屏蔽引号内的影响
        result.addAll(parseArguments(argsPart));

        return result;
    }

    private static List<String> parseArguments(String argsPart) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean insideQuotes = false; // 标记是否在引号内
        char prevChar = '\0'; // 上一个字符，用于判断转义字符

        for (char c : argsPart.toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                // 遇到未转义的双引号，切换引号状态
                insideQuotes = !insideQuotes;
            } else if (c == ' ' && !insideQuotes) {
                // 遇到空格且不在引号内，分隔参数
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0); // 清空 StringBuilder
                }
            } else {
                // 添加当前字符
                currentArg.append(c);
            }
            prevChar = c;
        }

        // 添加最后一个参数
        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }

        // 处理转义字符
        for (int i = 0; i < args.size(); i++) {
            args.set(i, processEscapedCharacters(args.get(i)));
        }

        return args;
    }

    private static String processEscapedCharacters(String input) {
        // 处理常见的转义字符
        return input.replace("\\\"", "\"")  // 转换转义的双引号
                .replace("\\\\", "\\") // 转换转义的反斜杠
                .replace("\\n", "\n")  // 转换换行符
                .replace("\\t", "\t"); // 转换制表符
    }
    public static void main(String[] args) {
        List<String> tests = getSpellParts("outputHtml \"<!DOCTYPE html>\\n<html lang=\\\"zh-CN\\\">\\n<head>\\n \" \"hello\"");
        for (String test : tests) {
            System.out.println(test);
        }
    }

    public static List<String> findSpells(String text) {
        List<String> spells = new ArrayList<String>(); // JDK 7 需要显式指定泛型类型
        // 使用 Pattern.DOTALL，使正则表达式的 . 能匹配换行符
        Pattern pattern = Pattern.compile("@#%(.*?)@#%", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            // 获取捕获组的内容并去掉前后空白字符
            String spell = matcher.group(1).trim();
            spells.add(spell);
        }
        return spells;
    }




}
