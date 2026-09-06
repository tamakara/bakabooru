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
  NSkeleton,
  NSpin,
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
import {galleryApi, type ImageDto, type ImageTagDto} from '../../api/gallery.ts'
import {tagsApi} from '../../api/tags.ts'
import {useDateFormat} from '@vueuse/core'

const props = defineProps<{
  show: boolean
  image: ImageDto | null
  loading?: boolean
  hasPrev?: boolean
  hasNext?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'update:image', value: ImageDto | null): void
  (e: 'refresh'): void
  (e: 'prev'): void
  (e: 'next'): void
}>()

const message = useMessage()
const editingName = ref(false)
const newName = ref('')
const newTagName = ref('')
const tagSearchOptions = ref<AutoCompleteOption[]>([])
const isEditingTags = ref(false)
const addingTag = ref(false)
const retryingAi = ref(false)
const recomputing = ref(false)

// 褰搃mage鍙樺寲鏃舵洿鏂扮紪杈戣〃鍗?
watch(() => props.image, (newImage) => {
  if (newImage) {
    newName.value = newImage.title
  }
}, {immediate: true})


const tagTypeOrder = ['copyright', 'character', 'artist', 'general', 'meta', 'rating', 'year']

const tagTypeMap: Record<string, string> = {
  copyright: '鐗堟潈',
  character: '瑙掕壊',
  artist: '浣滆€?,
  general: '涓€鑸?,
  meta: '鍏冩暟鎹?,
  rating: '鍒嗙骇',
  year: '骞翠唤',
}

const formattedSize = computed(() => {
  if (!props.image) return ''
  const size = props.image.size
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
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
  if (!props.image || !newName.value || newName.value === props.image.title) {
    editingName.value = false
    return
  }
  try {
    const updated = await galleryApi.updateImage(props.image.id, {title: newName.value})
    emit('update:image', updated)
    message.success('鍚嶇О宸叉洿鏂?)
    emit('refresh')
  } catch (e) {
    message.error('鏇存柊鍚嶇О澶辫触')
  } finally {
    editingName.value = false
  }
}

const handleDelete = async () => {
  if (!props.image) return
  try {
    await galleryApi.deleteImage(props.image.id)
    message.success('鍥剧墖宸插垹闄?)
    emit('refresh')
    handleClose()
  } catch (e) {
    message.error('鍒犻櫎鍥剧墖澶辫触')
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

  if (!props.image || !tagName) {
    return
  }

  addingTag.value = true
  try {
    const existingTags = await tagsApi.listTags(tagName)
    const targetTag = existingTags.find(t => t.name.toLowerCase() === tagName.toLowerCase())

    if (!targetTag) {
      message.error('娣诲姞澶辫触锛氭爣绛句笉瀛樺湪锛屽彧鑳芥坊鍔犳暟鎹簱涓凡鏈夋爣绛?)
      return
    }

    if (props.image.tags.some(t => t.id === targetTag.id)) {
      message.warning('璇ユ爣绛惧凡娣诲姞')
      newTagName.value = ''
      return
    }

    const updated = await galleryApi.addTag(props.image.id, targetTag.id)
    emit('update:image', updated)
    message.success('鏍囩娣诲姞鎴愬姛')
    newTagName.value = ''
    tagSearchOptions.value = []
  } catch (e) {
    message.error('鏍囩娣诲姞澶辫触')
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

const handleRemoveTag = async (tag: ImageTagDto) => {
  if (!props.image) return
  try {
    const updated = await galleryApi.removeTag(props.image.id, tag.id)
    emit('update:image', updated)
    message.success('鏍囩宸茬Щ闄?)
  } catch (e) {
    message.error('绉婚櫎鏍囩澶辫触')
  }
}

const handleDownload = () => {
  if (!props.image) return
  const link = document.createElement('a')
  link.href = props.image.imageUrl
  link.download = props.image.fileName || (props.image.title + '.' + props.image.extension)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const statusText = computed(() => {
  if (!props.image) return ''
  if (props.image.status === 'AVAILABLE') return '宸插畬鎴?
  if (props.image.status === 'PROCESSING') return '澶勭悊涓?
  if (props.image.status === 'MISSING') return '澶勭悊澶辫触'
  return '寰呭鐞?
})

const statusType = computed<'default' | 'success' | 'info' | 'warning' | 'error'>(() => {
  if (!props.image) return 'default'
  if (props.image.status === 'AVAILABLE') return 'success'
  if (props.image.status === 'PROCESSING') return 'info'
  if (props.image.status === 'MISSING') return 'error'
  return 'default'
})

const canRetryAi = computed(() => {
  return !!props.image && props.image.status === 'MISSING'
})

const handleRetryAi = async () => {
  if (!props.image || retryingAi.value) return
  retryingAi.value = true
  try {
    const updated = await galleryApi.retryAiProcessing(props.image.id)
    emit('update:image', updated)
    emit('refresh')
    message.success('宸插紑濮?AI 澶勭悊')
  } catch (e) {
    message.error('閲嶈瘯 AI 澶勭悊澶辫触')
  } finally {
    retryingAi.value = false
  }
}

const handleRecomputeTags = async () => {
  if (!props.image || !props.image.tagModelId || recomputing.value) return
  recomputing.value = true
  try {
    const updated = await galleryApi.generateTags(props.image.id, props.image.tagModelId)
    emit('update:image', updated)
    emit('refresh')
    message.success('宸查噸鏂版帓闃熸爣绛剧敓鎴?)
  } catch {
    message.error('鏍囩鐢熸垚鎺掗槦澶辫触')
  } finally { recomputing.value = false }
}

const handleRecomputeVectors = async () => {
  if (!props.image || !props.image.indexVectors?.length || recomputing.value) return
  recomputing.value = true
  try {
    const updated = await galleryApi.generateVectors(props.image.id, props.image.indexVectors.map(v => v.modelId))
    emit('update:image', updated)
    emit('refresh')
    message.success('宸查噸鏂版帓闃熷悜閲忚绠?)
  } catch {
    message.error('鍚戦噺璁＄畻鎺掗槦澶辫触')
  } finally { recomputing.value = false }
}

const groupedTags = computed(() => {
  if (!props.image || !props.image.tags) return {}

  const groups: Record<string, ImageTagDto[]> = {}
  tagTypeOrder.forEach(t => groups[t] = [])

  props.image.tags.forEach(tag => {
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

        <!-- 涓诲浘鐗囧尯鍩?-->
        <div
            class="flex-none w-full h-[60vh] lg:h-full lg:flex-1 lg:w-auto flex items-center justify-center bg-black overflow-hidden group sticky top-0 lg:relative z-0">

          <!-- 鍔犺浇鐘舵€?-->
          <n-spin v-if="props.loading" size="large" description="鍔犺浇涓?.." />

          <n-image
              v-else-if="props.image"
              :src="props.image.imageUrl"
              :alt="props.image.title"
              class="w-full h-full flex items-center justify-center"
              object-fit="contain"
              :img-props="{ class: 'max-h-full max-w-full object-contain' }"
          />

          <!-- 瀵艰埅鎸夐挳 -->
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

        <!-- 淇℃伅闈㈡澘 -->
        <div
            class="flex-none w-full lg:w-[400px] lg:h-full bg-gray-900 border-t lg:border-t-0 lg:border-l border-gray-800 flex flex-col relative shadow-2xl z-20 min-h-[40vh]">

          <!-- 椤堕儴鎿嶄綔鏍?-->
          <div v-if="props.loading" class="p-4 border-b border-gray-800 bg-gray-900 shrink-0">
            <div class="grid grid-cols-3 gap-2">
              <n-skeleton height="34px" :sharp="false" />
              <n-skeleton height="34px" :sharp="false" />
              <n-skeleton height="34px" :sharp="false" />
            </div>
          </div>
          <div v-else-if="props.image" class="p-4 border-b border-gray-800 bg-gray-900 shrink-0">
            <div class="grid grid-cols-3 gap-2">
              <n-popconfirm @positive-click="handleDelete" placement="bottom">
                <template #trigger>
                  <n-button secondary type="error" block>
                    <template #icon>
                      <n-icon :component="TrashOutline"/>
                    </template>
                    鍒犻櫎
                  </n-button>
                </template>
                纭畾瑕佸垹闄よ繖寮犲浘鐗囧悧锛?
              </n-popconfirm>

              <n-button secondary type="info" block @click="handleDownload">
                <template #icon>
                  <n-icon :component="DownloadOutline"/>
                </template>
                涓嬭浇
              </n-button>

              <n-button secondary block @click="handleClose">
                <template #icon>
                  <n-icon :component="CloseOutline"/>
                </template>
                鍏抽棴
              </n-button>
            </div>
          </div>

          <!-- 鍔犺浇鐘舵€佷笅鐨勫唴瀹归鏋?-->
          <div v-if="props.loading" class="flex-1 lg:overflow-y-auto p-6 flex flex-col custom-scrollbar gap-6">
            <!-- 鏍囬楠ㄦ灦 -->
            <div class="flex flex-col gap-2">
              <n-skeleton text style="width: 60px" />
              <n-skeleton text style="width: 80%" :repeat="1" />
            </div>

            <n-divider class="my-0 bg-gray-800"/>

            <!-- 璇︾粏淇℃伅楠ㄦ灦 -->
            <div class="flex flex-col gap-4">
              <n-skeleton text style="width: 80px" />
              <div class="grid grid-cols-2 gap-y-5 gap-x-4">
                <div class="flex flex-col gap-1" v-for="i in 5" :key="i">
                  <n-skeleton text style="width: 60px" />
                  <n-skeleton text style="width: 100px" />
                </div>
              </div>
              <div class="flex flex-col gap-3 mt-2">
                <div class="flex flex-col gap-1">
                  <n-skeleton text style="width: 50px" />
                  <n-skeleton height="36px" :sharp="false" />
                </div>
                <div class="flex flex-col gap-1">
                  <n-skeleton text style="width: 40px" />
                  <n-skeleton height="36px" :sharp="false" />
                </div>
              </div>
            </div>

            <n-divider class="my-0 bg-gray-800"/>

            <!-- 鏍囩楠ㄦ灦 -->
            <div class="flex flex-col gap-3">
              <n-skeleton text style="width: 50px" />
              <div class="flex flex-wrap gap-2">
                <n-skeleton height="24px" width="60px" :sharp="false" v-for="i in 6" :key="i" />
              </div>
            </div>
          </div>

          <div v-else-if="props.image" class="flex-1 lg:overflow-y-auto p-6 flex flex-col custom-scrollbar">
            <!-- 鏍囬閮ㄥ垎 -->
            <div class="flex flex-col gap-2">
              <div class="text-sm text-gray-400 uppercase font-bold tracking-wider">鏍囬</div>
              <div v-if="!editingName" @click="editingName = true"
                   class="text-xl lg:text-2xl font-semibold cursor-pointer hover:text-primary-400 break-words transition-colors"
                   title="鐐瑰嚮缂栬緫">
                {{ props.image.title }}
              </div>
              <n-input
                  v-else v-model:value="newName"
                  @blur="saveName"
                  @keyup.enter="saveName"
                  autofocus
                  placeholder="杈撳叆鍚嶇О" size="large"
              />
            </div>

            <n-divider class="my-0 bg-gray-800"/>

            <!-- 璇︾粏淇℃伅 -->
            <div class="flex flex-col gap-4">
              <div class="text-sm text-gray-400 uppercase font-bold tracking-wider">璇︾粏淇℃伅</div>
              <div class="grid grid-cols-2 gap-y-5 gap-x-4 text-sm lg:text-base">
                <!-- Size -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="ResizeOutline"/> 灏哄
                      </span>
                  <span class="text-gray-200 font-mono">{{ props.image.width }} 脳 {{ props.image.height }}</span>
                </div>
                <!-- View Count -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="EyeOutline"/> 鏌ョ湅娆℃暟
                      </span>
                  <span class="text-gray-200 font-mono">{{ props.image.viewCount || 0 }}</span>
                </div>
                <!-- File Size -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="HardwareChipOutline"/> 澶у皬
                      </span>
                  <span class="text-gray-200 font-mono">{{ formattedSize }}</span>
                </div>
                <!-- Format -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="ImageOutline"/> 鏍煎紡
                      </span>
                  <span class="text-gray-200 uppercase font-mono">{{ props.image.extension }}</span>
                </div>
                <!-- Date -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="TimeOutline"/> 鍒涘缓鏃堕棿
                      </span>
                  <span class="text-gray-200 font-mono">{{
                      useDateFormat(props.image.createdAt, 'YYYY-MM-DD').value
                    }}</span>
                </div>
                <!-- AI Status -->
                <div class="flex flex-col gap-1">
                      <span class="text-gray-500 text-xs flex items-center gap-1">
                         <n-icon :component="HardwareChipOutline"/> AI 鐘舵€?
                      </span>
                  <div class="flex items-center gap-2">
                    <n-tag size="small" :type="statusType" :bordered="false">
                      {{ statusText }}
                    </n-tag>
                    <n-button
                        v-if="canRetryAi"
                        size="tiny"
                        secondary
                        type="warning"
                        :loading="retryingAi"
                        @click="handleRetryAi"
                    >
                      <template #icon>
                        <n-icon :component="RefreshOutline"/>
                      </template>
                      閲嶈瘯
                    </n-button>
                  </div>
                </div>
              </div>

              <div v-if="props.image.aiError" class="text-xs text-amber-300 bg-amber-950/40 border border-amber-900/60 rounded p-2 break-words">
                {{ props.image.aiError }}
              </div>

              <div class="grid grid-cols-1 gap-2 text-sm">
                <div>
                  <div class="flex items-center justify-between">
                    <span class="text-gray-500 text-xs">绱㈠紩鍚戦噺</span>
                    <n-button v-if="props.image.indexVectors?.length" size="tiny" text :loading="recomputing" @click="handleRecomputeVectors">閲嶆柊璁＄畻</n-button>
                  </div>
                  <div class="flex flex-wrap gap-1 mt-1">
                    <n-tag v-for="vector in props.image.indexVectors || []" :key="`${vector.modelId}-${vector.modelRevision}`" size="small" :bordered="false">
                      {{ vector.modelId }} 路 {{ vector.status }}
                    </n-tag>
                    <span v-if="!props.image.indexVectors?.length" class="text-gray-500 text-xs">鏈绠?/span>
                  </div>
                </div>
                <div>
                  <div class="flex items-center justify-between">
                    <span class="text-gray-500 text-xs">鏍囩鐢熸垚妯″瀷</span>
                    <n-button v-if="props.image.tagModelId" size="tiny" text :loading="recomputing" @click="handleRecomputeTags">閲嶆柊鐢熸垚</n-button>
                  </div>
                  <div class="text-gray-200 mt-1">{{ props.image.tagModelId || '鏈娇鐢? }}</div>
                </div>
              </div>

              <!-- Full Filename & Hash -->
              <div class="flex flex-col gap-3 mt-2">
                <div class="flex flex-col gap-1">
                     <span class="text-gray-500 text-xs flex items-center gap-1">
                        <n-icon :component="DocumentTextOutline"/> 鏂囦欢鍚?
                     </span>
                  <n-tooltip trigger="hover" placement="top">
                    <template #trigger>
                       <span
                           class="text-gray-300 text-xs lg:text-sm truncate font-mono bg-black/30 p-2 rounded border border-gray-700/50 select-all block">{{
                           props.image.fileName
                         }}</span>
                    </template>
                    {{ props.image.fileName }}
                  </n-tooltip>
                </div>

                <div class="flex flex-col gap-1">
                     <span class="text-gray-500 text-xs flex items-center gap-1">
                        <span class="font-bold text-[10px]">#</span> 鍝堝笇
                     </span>
                  <n-tooltip trigger="hover" placement="top">
                    <template #trigger>
                       <span
                           class="text-gray-300 text-xs lg:text-sm truncate font-mono bg-black/30 p-2 rounded border border-gray-700/50 select-all block">{{
                           props.image.hash
                         }}</span>
                    </template>
                    {{ props.image.hash }}
                  </n-tooltip>
                </div>
              </div>
            </div>

            <n-divider class="my-0 bg-gray-800"/>

            <!-- 鏍囩閮ㄥ垎 -->
            <div class="flex flex-col gap-3">
              <div class="flex items-center justify-between">
                <div class="text-sm text-gray-400 uppercase font-bold tracking-wider flex items-center gap-1">
                  <n-icon :component="PricetagOutline"/>
                  鏍囩
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
                </div>
              </div>

              <div v-if="isEditingTags" class="mb-2">
                <n-input-group>
                  <n-auto-complete
                      v-model:value="newTagName"
                      :options="tagSearchOptions"
                      placeholder="杈撳叆鏍囩鍚嶇О..."
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

              <div v-if="props.image.tags?.length" class="flex flex-col gap-3">
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
              <span v-else class="text-gray-500 text-sm italic py-1">鏆傛棤鏍囩</span>
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



