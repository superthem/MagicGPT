package com.magicvector.ai;

import com.magicvector.ai.annotation.MagicBook;
import com.magicvector.ai.annotation.SpellDefinition;
import com.magicvector.ai.core.Spell;
import com.magicvector.ai.core.manager.SpellManager;
import com.magicvector.ai.core.register.impl.AnnotationCallSpellCollector;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class MagicApp {


    public static void start(String ... packages){
        for (String aPackage : packages) {
            log.info("正在扫描{}下的所有咒语并注册...", aPackage);
            loadAndRegister(aPackage);
        }
    }


    private static void loadAndRegister(String packageName){
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(MagicBook.class);
        annotatedClasses.forEach(clazz ->{
            // 获取类上的 MagicBook 注解
            MagicBook magicBook = clazz.getAnnotation(MagicBook.class);
            if (magicBook != null) {
                String bookName = magicBook.name(); // 获取 name 属性
                if (bookName == null || bookName.trim().isEmpty()) {
                    // 抛出异常
                    throw new IllegalArgumentException(
                            "Class " + clazz.getName() + " has an invalid @MagicBook annotation: 'name' cannot be null or empty"
                    );
                }

                // 查找类中所有被 @SpellDefinition 标记的方法
                Set<Method> methodSet = new HashSet<>();
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(SpellDefinition.class)) {
                        // 获取方法上的注解
                        SpellDefinition spellDefinition = method.getAnnotation(SpellDefinition.class);
                        methodSet.add(method);
                    }
                }

                List<Spell> candidates = new ArrayList<>();
                candidates.addAll(new AnnotationCallSpellCollector(methodSet).collect());
                for (Spell candidate : candidates) {
                    SpellManager.registerSpell(bookName, candidate);
                }
            }
        });
    }

}
