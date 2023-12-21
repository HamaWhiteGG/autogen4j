**Java version of Microsoft AutoGen, Enable Next-Gen Large Language Model Applications**

## 1. What is AutoGen

AutoGen is a framework that enables the development of LLM applications using multiple agents that can converse with each other to solve tasks. AutoGen agents are customizable, conversable, and seamlessly allow human participation. They can operate in various modes that employ combinations of LLMs, human inputs, and tools.

![AutoGen Overview](https://github.com/HamaWhiteGG/autogen4j/blob/dev/data/images/autogen_agentchat.png)

- AutoGen enables building next-gen LLM applications based on [multi-agent conversations](https://microsoft.github.io/autogen/docs/Use-Cases/agent_chat) with minimal effort. It simplifies the orchestration, automation, and optimization of a complex LLM workflow. It maximizes the performance of LLM models and overcomes their weaknesses.
- It supports [diverse conversation patterns](https://microsoft.github.io/autogen/docs/Use-Cases/agent_chat#supporting-diverse-conversation-patterns) for complex workflows. With customizable and conversable agents, developers can use AutoGen to build a wide range of conversation patterns concerning conversation autonomy,
  the number of agents, and agent conversation topology.
- It provides a collection of working systems with different complexities. These systems span a [wide range of applications](https://microsoft.github.io/autogen/docs/Use-Cases/agent_chat#diverse-applications-implemented-with-autogen) from various domains and complexities. This demonstrates how AutoGen can easily support diverse conversation patterns.

## 2. Quickstart

### 2.1 Maven Repository
Prerequisites for building:
* Java 17 or later
* Unix-like environment (we use Linux, Mac OS X)
* Maven (we recommend version 3.8.6 and require at least 3.5.4)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.hamawhitegg/autogen4j-core)](https://maven-badges.herokuapp.com/maven-central/io.github.hamawhitegg/autogen4j-core)
```xml
<dependency>
    <groupId>io.github.hamawhitegg</groupId>
    <artifactId>autogen4j-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2.2 Environment Setup
Using Autogen4j requires OpenAI's APIs, you need to set the environment variable.
```shell
export OPENAI_API_KEY=xxx

# If a proxy is needed, set the OPENAI_PROXY environment variable.
export OPENAI_PROXY=http://host:port
```

## 3. Multi-Agent Conversation Framework

Autogen enables the next-gen LLM applications with a generic [multi-agent conversation](https://microsoft.github.io/autogen/docs/Use-Cases/agent_chat) framework. It offers customizable and conversable agents that integrate LLMs, tools, and humans.
By automating chat among multiple capable agents, one can easily make them collectively perform tasks autonomously or with human feedback, including tasks that require using tools via code.

Features of this use case include:

- **Multi-agent conversations**: AutoGen agents can communicate with each other to solve tasks. This allows for more complex and sophisticated applications than would be possible with a single LLM.
- **Customization**: AutoGen agents can be customized to meet the specific needs of an application. This includes the ability to choose the LLMs to use, the types of human input to allow, and the tools to employ.
- **Human participation**: AutoGen seamlessly allows human participation. This means that humans can provide input and feedback to the agents as needed.

For [AutoFeedbackFromCodeExecutionExample](autogen4j-example/src/main/java/com/hw/autogen4j/example/AutoFeedbackFromCodeExecutionExample.java),
```java
// create an AssistantAgent named "assistant"
var assistant = AssistantAgent.builder()
        .name("assistant")
        .build();

var codeExecutionConfig = CodeExecutionConfig.builder()
        .workDir("data/coding")
        .build();
// create a UserProxyAgent instance named "user_proxy"
var userProxy = UserProxyAgent.builder()
        .name("user_proxy")
        .humanInputMode(NEVER)
        .maxConsecutiveAutoReply(10)
        .isTerminationMsg(e -> e.getContent().strip().endsWith("TERMINATE"))
        .codeExecutionConfig(codeExecutionConfig)
        .build();

// the assistant receives a message from the user_proxy, which contains the task description
userProxy.initiateChat(assistant,
        "What date is today? Compare the year-to-date gain for META and TESLA.");

// followup of the previous question
userProxy.send(assistant,
        "Plot a chart of their stock price change YTD and save to stock_price_ytd.png.");
```

The figure below shows an example conversation flow with AutoGen.
![Agent Chat Example](https://github.com/HamaWhiteGG/autogen4j/blob/dev/data/images/chat_example.png)

## 4. Run Test Cases from Source

```shell
git clone https://github.com/HamaWhiteGG/autogen4j.git
cd autogen4j

# export JAVA_HOME=JDK17_INSTALL_HOME && mvn clean test
mvn clean test
```

This project uses Spotless to format the code. If you make any modifications, please remember to format the code using the following command.

```shell
# export JAVA_HOME=JDK17_INSTALL_HOME && mvn spotless:apply
mvn spotless:apply
```