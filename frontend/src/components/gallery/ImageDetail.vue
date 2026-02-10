<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {
  NButton,
  NDivider,
  NIcon,
  NImage,
  NInput,
  NInputGroup,
  NModal,
  NPopconfirm,
  NTag,
  NTooltip,
  useMessage,
  NAutoComplete,
  type AutoCompleteOption
} from 'naive-ui'
import {
  AddOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  CloseOutline,
  DocumentTextOutline,
  DownloadOutline,
  EyeOutline,
  HardwareChipOutline,
  ImageOutline,
  PencilOutline,
  PricetagOutline,
  RefreshOutline,
  ResizeOutline,
  TimeOutline,
  TrashOutline
} from '@vicons/ionicons5'
import {galleryApi, type ImageDto, type TagDto} from '../../api/gallery.ts'
import {tagsApi} from '../../api/tags.ts'
import {useDateFormat} from '@vueuse/core'

const props = defineProps<{
  show: boolean
  imageId: number | null
  hasPrev?: boolean
  hasNext?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'refresh'): void
  (e: 'prev'): void
  (e: 'next'): void
}>()

const message = useMessage()
const image = ref<ImageDto | null>(null)
const loading = ref(false)
const editingName = ref(false)
const newName = ref('')
const newTagName = ref('')
const tagSearchOptions = ref<AutoCompleteOption[]>([])
const regenerating = ref(false)
const isEditingTags = ref(false)
const addingTag = ref(false)


const tagTypeOrder = ['copyright', 'character', 'artist', 'general', 'meta', 'rating', 'year']

const tagTypeMap: Record<string, string> = {
  copyright: '版权',
  character: '角色',
  artist: '作者',
  general: '一般',
  meta: '元数据',
  rating: '分级',
  year: '年份',
}

const formattedSize = computed(() => {
  if (!image.value) return ''
  const size = image.value.size
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
})

const fetchImage = async () => {
  if (!props.imageId) return
  image.value = null
  loading.value = true
  try {
    image.value = await galleryApi.getImage(props.imageId)
    if (image.value) {
      newName.value = image.value.title
    }
  } catch (e) {
    message.error('加载图片详情失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.imageId, () => {
  if (props.show && props.imageId) {
    fetchImage()
  }
})

watch(() => props.show, (val) => {
  if (val && props.imageId) {
    fetchImage()
  } else {
    // keeping image data slightly longer for transition or clear it
    // image.value = null
  }
})

const handlePrev = () => {
  if (props.hasPrev) emit('prev')
}

const handleNext = () => {
  if (props.hasNext) emit('next')
}

const handleKeydown = (e: KeyboardEvent) => {
  if (!props.show) return
  if (e.key === 'ArrowLeft') handlePrev()
  if (e.key === 'ArrowRight') handleNext()
  if (e.key === 'Escape') handleClose()
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const handleClose = () => {
  emit('update:show', false)
}


const saveName = async () => {
  if (!image.value || !newName.value || newName.value === image.value.title) {
    editingName.value = false
    return
  }
  try {
    image.value = await galleryApi.updateImage(image.value.id, {title: newName.value})
    message.success('名称已更新')
    emit('refresh')
  } catch (e) {
    message.error('更新名称失败')
  } finally {
    editingName.value = false
  }
}

const handleDelete = async () => {
  if (!image.value) return
  try {
    await galleryApi.deleteImage(image.value.id)
    message.success('图片已删除')
    emit('refresh')
    handleClose()
  } catch (e) {
    message.error('删除图片失败')
  }
}

const handleRegenerate = async () => {
  if (!image.value) return
  regenerating.value = true
  try {
    image.value = await galleryApi.regenerateTags(image.value.id)
    message.success('标签已重新生成')
  } catch (e) {
    message.error('重新生成标签失败')
  } finally {
    regenerating.value = false
  }
}

const handleTagSearch = async (value: string) => {
  newTagName.value = value
  if (!value || !value.trim()) {
    tagSearchOptions.value = []
    return
  }

  try {
    const tags = await tagsApi.listTags(value)
    tagSearchOptions.value = tags.map(t => ({
      label: t.name,
      value: t.name
    }))
  } catch (e) {
    tagSearchOptions.value = []
  }
}

const handleAddTag = async (value?: string | any) => {
  if (addingTag.value) return

  let tagName = typeof value === 'string' ? value : newTagName.value
  tagName = tagName?.trim()

  if (!image.value || !tagName) {
    return
  }

  addingTag.value = true
  try {
    const existingTags = await tagsApi.listTags(tagName)
    const targetTag = existingTags.find(t => t.name.toLowerCase() === tagName.toLowerCase())

    if (!targetTag) {
      message.error('添加失败：标签不存在，只能添加数据库中已有标签')
      return
    }

    if (image.value.tags.some(t => t.id === targetTag.id)) {
      message.warning('该标签已添加')
      newTagName.value = ''
      return
    }

    image.value = await galleryApi.addTag(image.value.id, {
      name: targetTag.name,
      type: targetTag.type
    })
    message.success('标签添加成功')
    newTagName.value = ''
    tagSearchOptions.value = []
  } catch (e) {
    message.error('标签添加失败')
  } finally {
    addingTag.value = false
  }
}

const handleSelect = (value: string | number) => {
  newTagName.value = String(value)
  tagSearchOptions.value = []
}

const handleEnter = (e: KeyboardEvent) => {
  // If list is visible, autocomplete with the first option
  if (tagSearchOptions.value.length > 0) {
    const firstOption = tagSearchOptions.value[0]
    if (firstOption) {
      newTagName.value = String(firstOption.value)
    }
    tagSearchOptions.value = []
    e.preventDefault()
    return
  }

  // If list is hidden/empty, try to submit
  const currentVal = newTagName.value.trim()
  if (!currentVal) return
  handleAddTag()
}

const handleRemoveTag = async (tag: TagDto) => {
  if (!image.value) return
  try {
    image.value = await galleryApi.removeTag(image.value.id, tag.id)
    message.success('标签已移除')
  } catch (e) {
    message.error('移除标签失败')
  }
}

const handleDownload = () => {
  if (!image.value) return
  const link = document.createElement('a')
  link.href = image.value.url
  link.download = image.value.fileName || (image.value.title + '.' + image.value.extension)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const groupedTags = computed(() => {
  if (!image.value || !image.value.tags) return {}

  const groups: Record<string, TagDto[]> = {}
  tagTypeOrder.forEach(t => groups[t] = [])

  image.value.tags.forEach(tag => {
    const type = tag.type || 'general'
    if (groups[type]) {
      groups[type].push(tag)
    } else {
      if (!groups['general']) groups['general'] = []
      groups['general'].push(tag)
    }
  })

  return groups
})

const getTagColor = (type: string) => {
  switch (type) {
    case 'custom':
      return {color: 'rgba(0, 188, 212, 0.15)', textColor: '#00bcd4'}
    case 'copyright':
      return {color: 'rgba(213, 0, 249, 0.15)', textColor: '#e040fb'}
    case 'character':
      return {color: 'rgba(0, 200, 83, 0.15)', textColor: '#69f0ae'}
    case 'artist':
      return {color: 'rgba(255, 23, 68, 0.15)', textColor: '#ff5252'}
    case 'meta':
      return {color: 'rgba(255, 145, 0, 0.15)', textColor: '#ffab40'}
    case 'rating':
      return {color: 'rgba(158, 158, 158, 0.15)', textColor: '#bdbdbd'}
    case 'year':
      return {color: 'rgba(124, 77, 255, 0.15)', textColor: '#b388ff'}
    case 'general':
    default:
      return {color: 'rgba(59, 130, 246, 0.15)', textColor: '#93c5fd'}
  }
}

</script>

<template>
  <n-modal :show="show" @update:show="(v) => emit('update:show', v)" class="!m-0 !p-0" :auto-focus="false">
    <div class="flex flex-col w-screen h-screen bg-black text-white overflow-hidden">

      <!-- Content Area -->
      <div
          class="flex-1 flex flex-col lg:flex-row w-full h-full lg:overflow-hidden overflow-y-auto custom-scrollbar relative">

        <!-- 主图片区域 -->
        <div
            class="flex-none w-full h-[60vh] lg:h-full lg:flex-1 lg:w-auto flex items-center justify-center bg-black overflow-hidden group sticky top-0 lg:relative z-0">
          <div v-if="loading" class="absolute inset-0 flex items-center justify-center z-20">
            <div class="w-10 h-10 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          </div>

          <n-image
              v-if="image"
              :src="image.url"
              :alt="image.title"
              class="w-full h-full flex items-center justify-center"
              object-fit="contain"
              :img-props="{ class: 'max-h-full max-w-full object-contain' }"
          />

          <!-- 导航按钮 -->
          <div v-if="hasPrev"
               class="absolute left-4 top-1/2 -translate-y-1/2 z-10 p-2 rounded-full bg-black/50 hover:bg-black/80 text-white cursor-pointer transition-all flex items-center justify-center aspect-square"
               @click.stop="handlePrev">
            <n-icon size="40" :component="ChevronBackOutline"/>
          </div>

          <div v-if="hasNext"
               class="absolute right-4 top-1/2 -translate-y-1/2 z-10 p-2 rounded-full bg-black/50 hover:bg-black/80 text-white cursor-pointer transition-all flex items-center justify-center aspect-square"
               @click.stop="handleNext">
            <n-icon size="40" :component="ChevronForwardOutline"/>
          </div>
        </div>

        <!-- 信息面板 -->
        <div
            class="flex-none w-full lg:w-[400px] lg:h-full bg-gray-900 border-t lg:border-t-0 lg:border-l border-gray-800 flex flex-col relative shadow-2xl z-20 min-h-[40vh]">

          <!-- 顶部操作栏 -->
          <div v-if="image" class="p-4 border-b border-gray-800 bg-gray-900 shrink-0">
            <div class="grid grid-cols-3 gap-2">
              <n-popconfirm @positive-click="handleDelete" placement="bottom">
                <template #trigger>
                  <n-button secondary type="error" block>
                    <template #icon>
                      <n-icon :component="TrashOutline"/>
                    </template>
                    删除
                  </n-button>
                </template>
                确定要删除这张图片吗？
              </n-popconfirm>

              <n-button secondary type="info" block @click="handleDownload">
                <template #icon>
                  <n-icon :component="DownloadOutline"/>
                </template>
                下载
              </n-button>

              <n-button secondary block @click="handleClose">
                <template #icon>
                  <n-icon :component="CloseOutline"/>
                </template>
                关闭
              </n-button>
            </div>
          </div>

          <div v-if="image" class="flex-1 lg:overflow-y-auto p-6 flex flex-col custom-scrollbar">
            <!-- 标题部分 -->
            <div class="flex flex-col gap-2">
              <div class="text-sm text-gray-400 uppercase font-bold tracking-wider">标题</div>
              <div v-if="!editingName" @click="editingName = true"
                   class="text-xl lg:text-2xl font-semibold cursor-pointer hover:text-primary-400 break-words transition-colors"
                   title="点击编辑">
                {{ image.title }}
              </div>
              <n-input v-else v-model:value="newName" @blur="saveName" @keyup.enter="saveName" autofocus
                       placeholder="输入名称" size="large"/>
            </div>

            <n-divider class="my-0 bg-gray-800"/>

            <!-- 详细信息 -->
            <div class="flex flex-col gap-4">
              <div class="text-sm text-gray-400 uppercase font-bold tracking-wider">详细信息</div>
              <div class="grid grid-cols-2 gap-y-5 gap-x-4 text-sm lg:text-base">
                <!-- Size -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="ResizeOutline"/> 尺寸
                      </span>
                  <span class="text-gray-200 font-mono">{{ image.width }} × {{ image.height }}</span>
                </div>
                <!-- View Count -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="EyeOutline"/> 查看次数
                      </span>
                  <span class="text-gray-200 font-mono">{{ image.viewCount || 0 }}</span>
                </div>
                <!-- File Size -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="HardwareChipOutline"/> 大小
                      </span>
                  <span class="text-gray-200 font-mono">{{ formattedSize }}</span>
                </div>
                <!-- Format -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="ImageOutline"/> 格式
                      </span>
                  <span class="text-gray-200 uppercase font-mono">{{ image.extension }}</span>
                </div>
                <!-- Date -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="TimeOutline"/> 创建时间
                      </span>
                  <span class="text-gray-200 font-mono">{{ useDateFormat(image.createdAt, 'YYYY-MM-DD').value }}</span>
                </div>
              </div>

              <!-- Full Filename & Hash -->
              <div class="flex flex-col gap-3 mt-2">
                <div class="flex flex-col gap-1">
                     <span class="text-gray-500 text-xs flex items-center gap-1">
                        <n-icon :component="DocumentTextOutline"/> 文件名
                     </span>
                  <n-tooltip trigger="hover" placement="top">
                    <template #trigger>
                       <span
                           class="text-gray-300 text-xs lg:text-sm truncate font-mono bg-black/30 p-2 rounded border border-gray-700/50 select-all block">{{
                           image.fileName
                         }}</span>
                    </template>
                    {{ image.fileName }}
                  </n-tooltip>
                </div>

                <div class="flex flex-col gap-1">
                     <span class="text-gray-500 text-xs flex items-center gap-1">
                        <span class="font-bold text-[10px]">#</span> 哈希
                     </span>
                  <n-tooltip trigger="hover" placement="top">
                    <template #trigger>
                       <span
                           class="text-gray-300 text-xs lg:text-sm truncate font-mono bg-black/30 p-2 rounded border border-gray-700/50 select-all block">{{
                           image.hash
                         }}</span>
                    </template>
                    {{ image.hash }}
                  </n-tooltip>
                </div>
              </div>
            </div>

            <n-divider class="my-0 bg-gray-800"/>

            <!-- 标签部分 -->
            <div class="flex flex-col gap-3">
              <div class="flex items-center justify-between">
                <div class="text-sm text-gray-400 uppercase font-bold tracking-wider flex items-center gap-1">
                  <n-icon :component="PricetagOutline"/>
                  标签
                </div>
                <div class="flex gap-2">
                  <n-button
                      size="tiny"
                      secondary
                      circle
                      :type="isEditingTags ? 'warning' : 'tertiary'"
                      @click="isEditingTags = !isEditingTags"
                  >
                    <template #icon>
                      <n-icon :component="PencilOutline"/>
                    </template>
                  </n-button>
                  <n-button
                      size="tiny"
                      secondary
                      circle
                      type="info"
                      :loading="regenerating"
                      @click="handleRegenerate"
                  >
                    <template #icon>
                      <n-icon :component="RefreshOutline"/>
                    </template>
                  </n-button>
                </div>
              </div>

              <div v-if="isEditingTags" class="mb-2">
                <n-input-group>
                  <n-auto-complete
                      v-model:value="newTagName"
                      :options="tagSearchOptions"
                      placeholder="输入标签名称..."
                      size="small"
                      clearable
                      @update:value="handleTagSearch"
                      @select="handleSelect"
                      @keydown.enter="handleEnter"
                  />
                  <n-button size="small" type="primary" secondary @click="handleAddTag">
                    <template #icon>
                      <n-icon :component="AddOutline"/>
                    </template>
                  </n-button>
                </n-input-group>
              </div>

              <div v-if="image.tags?.length" class="flex flex-col gap-3">
                <template v-for="type in tagTypeOrder" :key="type">
                  <div v-if="groupedTags[type]?.length" class="flex flex-col gap-1">
                    <div class="text-xs text-gray-500 uppercase font-semibold tracking-wider ml-1">
                      {{ tagTypeMap[type] || type }}
                    </div>
                    <div class="flex flex-wrap gap-2">
                      <n-tag
                          v-for="tag in groupedTags[type]"
                          :key="tag.id"
                          size="small"
                          round
                          :bordered="false"
                          :color="getTagColor(type)"
                          :closable="isEditingTags"
                          @close="handleRemoveTag(tag)"
                          class="hover:opacity-80 transition-opacity"
                      >
                        {{ tag.name }}
                      </n-tag>
                    </div>
                  </div>
                </template>
              </div>
              <span v-else class="text-gray-500 text-sm italic py-1">暂无标签</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #4b5563;
  border-radius: 3px;
}
</style>

