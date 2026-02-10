<script setup lang="ts">
import {ref} from 'vue'
import {NAutoComplete, NInput, type AutoCompleteOption} from 'naive-ui'
import {tagsApi} from '../../api/tags.ts'

const props = defineProps<{
  value: string
  placeholder: string
  autosize: boolean | {
    minRows?: number;
    maxRows?: number;
  };
}>()

const emit = defineEmits<{
  (e: 'update:value', value: string): void
  (e: 'search'): void
}>()

const tagOptions = ref<AutoCompleteOption[]>([])

const isSelecting = ref(false)

/**
 * 处理选中标签
 * 手动拼接字符串，实现替换当前正在输入的标签，而不是简单追加
 */
function handleSelect(value: string) {
  isSelecting.value = true
  // 使用正则分割，保留分隔符，以便重建前缀
  const parts = props.value.split(/(\s+)/)
  // 移除最后一个部分（即当前正在输入的标签片段）
  parts.pop()
  const prefix = parts.join('')

  emit('update:value', prefix + value + ' ')

  tagOptions.value = []
  setTimeout(() => {
    isSelecting.value = false
  }, 0)
}

/**
 * 处理输入值更新
 * 实时搜索标签建议
 */
async function handleUpdateValue(value: string) {
  if (isSelecting.value) return
  emit('update:value', value)

  if (!value || !value.trim()) {
    tagOptions.value = []
    return
  }

  // 获取当前光标所在的最后一个单词
  const parts = value.split(/(\s+)/)
  const lastWord = parts.pop() || ''

  const isExclude = lastWord.startsWith('-')
  const query = isExclude ? lastWord.substring(1) : lastWord

  if (!query) {
    tagOptions.value = []
    return
  }

  try {
    const tags = await tagsApi.listTags(query)

    const options = tags.map(t => {
      const label = (isExclude ? '-' : '') + t.name
      return {
        label: label,
        value: label
      }
    })

    // 如果只有一个选项且完全匹配当前输入则不显示
    const firstOption = options[0]
    if (options.length === 1 && firstOption?.label === lastWord) {
      tagOptions.value = []
    } else {
      tagOptions.value = options
    }
  } catch (e) {
    console.error(e)
    tagOptions.value = []
  }
}

function handleEnter(e: KeyboardEvent) {
  if (tagOptions.value.length === 0) {
    e.preventDefault()
    emit('search')
  }
}
</script>

<template>
  <n-auto-complete
      :value="props.value"
      :options="tagOptions"
      :placeholder="placeholder"
      :get-show="() => true"
      :append="true"
      @update:value="handleUpdateValue"
      @select="handleSelect"
  >
    <template #default="{ handleInput, handleBlur, handleFocus, value }">
      <n-input
          type="textarea"
          :value="value"
          :placeholder="placeholder"
          :autosize="autosize"
          @input="handleInput"
          @focus="handleFocus"
          @blur="handleBlur"
          @keydown.enter="handleEnter"
      />
    </template>
  </n-auto-complete>
</template>
