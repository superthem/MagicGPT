package com.magicvector.ai.core;

import com.magicvector.ai.model.Arg;
import lombok.Data;

import java.util.List;
@Data
public class Spell {

    private final String apiName;
    private final String description;
    private final String className;
    private final String methodName;
    /**
     * 需要GPT提供的参数
     */
    private final List<Arg> gptFeedArgs;

    private Spell(Builder builder) {
        this.apiName = builder.apiName;
        this.description = builder.description;
        this.className = builder.className;
        this.methodName = builder.methodName;
        this.gptFeedArgs = builder.feedArgs;
    }


    public static class Builder {
        private String apiName;

        private String description;

        private String className;

        private String methodName;

        private List<Arg> feedArgs;

        public Builder setApiName(String apiName) {
            this.apiName = apiName;
            return this;
        }
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder setMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder setFeedArgs(List<Arg> feedArgs) {
            this.feedArgs = feedArgs;
            return this;
        }


        public Spell build() {
            return new Spell(this);
        }
    }
}
