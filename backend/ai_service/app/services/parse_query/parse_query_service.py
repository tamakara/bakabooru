from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from app.schema.parse_query_schema import ParsedTags
from app.services.parse_query.tag_matcher import TagMatcher
from app.services.parse_query.prompt import parse_query_prompt as prompt


class ParseQueryService:
    def __init__(self):
        print("正在初始化 TagMatcher...")
        self.matcher = TagMatcher(device="cpu")

    def parse(self, query: str, llm_url: str, llm_model: str, llm_api_key: str) -> str:
        # 初始化模型
        model = ChatOpenAI(
            model=llm_model,
            openai_api_base=llm_url,
            openai_api_key=llm_api_key,
            temperature=0
        )

        # 关键词提取
        parser = PydanticOutputParser(pydantic_object=ParsedTags)
        chain = prompt | model | parser
        parsed_tags: ParsedTags = chain.invoke({"user_input": query})
        print(parsed_tags)

        # 向量匹配
        result_tags = []

        # 匹配正向标签
        for word, tags in parsed_tags.positive.items():
            for tag in tags:
                match = self.matcher.match(tag, threshold=0.9)
                if match:
                    result_tags.append(match[0])
                    break

        # 匹配负向标签
        for word, tags in parsed_tags.negative.items():
            for tag in tags:
                match = self.matcher.match(tag, threshold=0.9)
                if match:
                    result_tags.append('-' + match[0])
                    break

        print(result_tags)

        # 拼接结果
        result = " ".join(result_tags)
        return result
