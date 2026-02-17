"""语义搜索服务 - 将自然语言转换为标签搜索条件和 CLIP 向量"""
from typing import List

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from app.core.model_manager import model_manager
from app.schemas.semantic_search import (
    SemanticSearchRequest,
    ParsedSemanticQuery,
    TagsResponse,
    EmbeddingResponse
)
from app.services.prompts import semantic_search_prompt
from app.services.tag_matcher_service import tag_matcher_service


class SemanticSearchService:
    """语义搜索服务"""

    def parse_tags(self, request: SemanticSearchRequest) -> TagsResponse:
        """
        解析语义描述为标签
        :param request: 语义搜索请求
        :return: 标签提取响应
        """
        try:
            # 1. 使用 LLM 解析语义描述
            parsed = self._parse_with_llm(
                query=request.query,
                llm_url=request.llm_url,
                llm_model=request.llm_model,
                llm_api_key=request.llm_api_key
            )

            # 2. 将解析的标签通过向量匹配转换为实际数据库标签
            positive_tags, negative_tags = self._match_tags(parsed)

            return TagsResponse.ok(positive=positive_tags, negative=negative_tags)

        except Exception as e:
            import traceback
            traceback.print_exc()
            return TagsResponse.fail(str(e))

    def generate_embedding(self, request: SemanticSearchRequest) -> EmbeddingResponse:
        """
        生成文本的 CLIP 向量
        :param request: 语义搜索请求
        :return: CLIP Embedding 响应
        """
        try:
            if not request.query:
                return EmbeddingResponse.ok()

            text = request.query
            print(f"生成 CLIP 嵌入: {text}")
            embedding = model_manager.encode_text_clip(text)
            embedding_list = embedding.flatten().tolist()

            return EmbeddingResponse.ok(text=text, embedding=embedding_list)

        except Exception as e:
            import traceback
            traceback.print_exc()
            return EmbeddingResponse.fail(str(e))

    def _parse_with_llm(
        self,
        query: str,
        llm_url: str,
        llm_model: str,
        llm_api_key: str
    ) -> ParsedSemanticQuery:
        """使用 LLM 解析语义描述"""
        model = ChatOpenAI(
            model=llm_model,
            openai_api_base=llm_url,
            openai_api_key=llm_api_key,
            temperature=0
        )

        parser = PydanticOutputParser(pydantic_object=ParsedSemanticQuery)
        chain = semantic_search_prompt | model | parser
        result = chain.invoke({"user_input": query})

        print(f"LLM 解析结果: {result}")
        return result

    def _match_tags(self, parsed: ParsedSemanticQuery) -> tuple[List[str], List[str]]:
        """将 LLM 解析的标签通过向量匹配转换为实际数据库标签"""
        positive_tags: List[str] = []
        negative_tags: List[str] = []

        # 匹配正向标签
        for keyword, tags in parsed.positive.items():
            for tag in tags:
                match = tag_matcher_service.match(tag, threshold=0.9)
                if match:
                    positive_tags.append(match[0])
                    break  # 找到一个匹配就跳到下一个关键词

        # 匹配负向标签
        for keyword, tags in parsed.negative.items():
            for tag in tags:
                match = tag_matcher_service.match(tag, threshold=0.9)
                if match:
                    negative_tags.append(match[0])
                    break

        print(f"匹配到的标签 - 正向: {positive_tags}, 负向: {negative_tags}")
        return positive_tags, negative_tags


# 单例服务实例
semantic_search_service = SemanticSearchService()
