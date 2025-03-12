package com.magicvector.ai.examples.timeReporter;

import com.magicvector.ai.MagicApp;
import com.magicvector.ai.output.SystemOutputStream;
import com.magicvector.ai.util.PromptUtil;
import com.github.tbwork.anole.loader.AnoleApp;
import com.github.tbwork.anole.loader.annotion.AnoleConfigLocation;
import com.magicvector.ai.MagicAgent;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@AnoleConfigLocation()
public class TestTimeReporter {


    public static void main(String[] args) {

        // 启动配置管理器Anole
        AnoleApp.start();

        MagicApp.start("com.magicvector.ai.examples.timeReporter");

        // 加载自定义提示词
        String headCustomPrompt = PromptUtil.readResourceByRelativePath("custom_prompts/time_reporter.prompt");

        // 指定包名搜索本地Call类型咒语
        MagicAgent agent = new MagicAgent("deepseek-chat", headCustomPrompt);

        System.out.print("AI：你好，当你需要知道现在几点了，随时问我!");

        // 开始聊天
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if(agent.isIdle()){
                System.out.print("\n我：");
                String input = scanner.nextLine();
                if(input.equals("exit")){
                    break;
                }
                System.out.print("AI：");
                // 推进一个聊天，指定一个输出流用于承载AI的输出
                agent.proceedWithStream(input, new SystemOutputStream());
            }

            try {
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
