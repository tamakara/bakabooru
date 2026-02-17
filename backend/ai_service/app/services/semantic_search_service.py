"""语义搜索服务 - 将自然语言转换为标签搜索条件和 CLIP 向量"""
from typing import List, Optional

from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from app.core.model_manager import model_manager
from app.schemas.semantic_search import (
    SemanticSearchRequest,
    SemanticSearchResponse,
    ParsedSemanticQuery,
    TagsResult,
    ClipSearchResult
)
from app.services.tag_matcher_service import tag_matcher_service
from app.services.prompts import semantic_search_prompt


class SemanticSearchService:
    """语义搜索服务"""

    def parse_and_embed(self, request: SemanticSearchRequest) -> SemanticSearchResponse:
        """
        解析语义描述并生成搜索条件
        :param request: 语义搜索请求
        :return: 包含标签和 CLIP 向量的搜索响应
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
            tags_result = self._match_tags(parsed)

            # 3. 如果有 CLIP 文本，生成 CLIP 嵌入
            clip_search = None
            if parsed.clip_text:
                clip_search = self._generate_clip_embedding(parsed.clip_text)

            return SemanticSearchResponse.ok(tags=tags_result, clip_search=clip_search)

        except Exception as e:
            import traceback
            traceback.print_exc()
            return SemanticSearchResponse.fail(str(e))

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

    def _match_tags(self, parsed: ParsedSemanticQuery) -> TagsResult:
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
        return TagsResult(positive=positive_tags, negative=negative_tags)

    def _generate_clip_embedding(self, text: str) -> ClipSearchResult:
        """生成 CLIP 文本嵌入"""
        print(f"生成 CLIP 嵌入: {text}")
        embedding = model_manager.encode_text_clip(text)
        embedding_list = embedding.cpu().numpy().flatten().tolist()

        return ClipSearchResult(
            text=text,
            embedding=embedding_list
        )


# 单例服务实例
semantic_search_service = SemanticSearchService()
