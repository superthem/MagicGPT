package com.magicvector.ai.util;

import com.magicvector.ai.core.manager.SpellManager;
import com.magicvector.ai.exceptions.MagicGPTGeneralException;
import com.magicvector.ai.model.Arg;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptUtil {


    public static String joinPrompt(String[] prompts, String delimiter) {
        if (prompts == null || prompts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(prompts[0]);
        for (int i = 1; i < prompts.length; i++) {
            sb.append(delimiter).append(prompts[i]);
        }
        return sb.toString();
    }





    public static String parseResource(String promptResourceName) {
        Map<String, String> promptMap = new HashMap<>();
        String language = null;
        StringBuilder promptBuilder = new StringBuilder();
        try (
                InputStream inputStream = PromptUtil.class.getClassLoader().getResourceAsStream(promptResourceName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                promptBuilder.append(line).append(System.lineSeparator());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return promptBuilder.toString();
    }



    public static String argToPrompt(Arg arg){

        String requireDesc = arg.isRequired() ? "不可为空":"可空";

        return "$"+arg.getName()+
                (StringUtil.isEmpty(arg.getDescription().trim())? "" : "("+requireDesc+"。"+arg.getDescription().trim()+")");

    }

    public static String join(String[] strings, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            sb.append(strings[i]);
            if (i < strings.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String readResourceByRelativePath(String relativePath) {
        try (InputStream inputStream = PromptUtil.class.getClassLoader().getResourceAsStream(relativePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource file not found: " + relativePath);
            }
            StringBuilder fileContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append(System.lineSeparator());
            }
            return fileContent.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 编译字符串中的表达式
     *
     * @param input 原始字符串
     * @return 编译后的字符串
     */
    public static String compileSpellPrompt(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 处理1类表达式 #{key}
        Pattern patternType1 = Pattern.compile("#\\{([^}]+)}");
        Matcher matcherType1 = patternType1.matcher(input);
        StringBuffer resultBuffer = new StringBuffer();
        while (matcherType1.find()) {
            String key = matcherType1.group(1); // 提取 key
            String value = getConfigValue(1, key); // 获取1类表达式的值
            matcherType1.appendReplacement(resultBuffer, value != null ? Matcher.quoteReplacement(value) : "");
        }
        matcherType1.appendTail(resultBuffer);

        // 处理2类表达式 ${key}
        String resultAfterType1 = resultBuffer.toString();
        Pattern patternType2 = Pattern.compile("@\\{([^}]+)}");
        Matcher matcherType2 = patternType2.matcher(resultAfterType1);
        resultBuffer.setLength(0); // 清空StringBuffer
        while (matcherType2.find()) {
            String key = matcherType2.group(1); // 提取 key
            String value = getConfigValue(2, key); // 获取2类表达式的值
            matcherType2.appendReplacement(resultBuffer, value != null ? Matcher.quoteReplacement(value) : "");
        }
        matcherType2.appendTail(resultBuffer);

        return resultBuffer.toString();
    }


    public static String compilePrompt(String prompt, Map<String, String> paramMap) {
        if (prompt == null || paramMap == null) {
            return prompt;
        }

        // 定义正则表达式，匹配类似于 ${key} 的占位符
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)}");
        Matcher matcher = pattern.matcher(prompt);

        // 用于存储替换结果
        StringBuffer resultBuffer = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1); // 获取占位符中的键
            String replacement = paramMap.getOrDefault(key, matcher.group(0)); // 获取替换值或保留原始占位符
            matcher.appendReplacement(resultBuffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resultBuffer); // 追加剩余部分

        return resultBuffer.toString();
    }

    public static String getConfigValue(Integer type, String key) {
        if (type == 1) {
            return SpellManager.getSpellBookPrompt(key);
        } else if (type == 2) {
            return SpellManager.getSpellPrompt(key);
        }
        throw new MagicGPTGeneralException("不存在的配置类型");
    }



    public static void main(String[] args) {
        // 测试用例
        String input = "This is a test #{a} and also a @{b} expression.";
        String compiled = compileSpellPrompt(input);
        System.out.println("Compiled String: " + compiled);
    }



}
