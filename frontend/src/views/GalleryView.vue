<script setup lang="ts">
import {useQuery} from '@tanstack/vue-query'
import {searchApi} from '../api/search'
import {galleryApi, type ImageDto, type ImageThumbnailDto} from '../api/gallery'
import {computed, h, nextTick, onMounted, reactive, ref, shallowRef, watch} from 'vue'
import {
  NButton,
  NDropdown,
  NEmpty,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NLayout,
  NLayoutContent,
  NLayoutFooter,
  NLayoutSider,
  NPagination,
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSlider,
  NSpace,
  NSpin,
  NTabPane,
  NTabs,
  NText,
  NUpload,
  NUploadDragger,
  useDialog,
  useMessage
} from 'naive-ui'
import ImageDetail from '../components/gallery/ImageDetail.vue'
import TagSearchInput from '../components/gallery/TagSearchInput.vue'
import {breakpointsTailwind, useBreakpoints} from '@vueuse/core'
import {Archive24Regular, CheckmarkCircle24Filled, Dismiss24Regular, Search24Regular} from '@vicons/fluent'
import {
  Checkbox,
  CloseCircleOutline,
  DownloadOutline,
  EyeOutline,
  FilterOutline,
  SquareOutline,
  TrashOutline
} from '@vicons/ionicons5'

import {v4 as uuidv4} from 'uuid';

// 搜索模式
const searchMode = ref<'TEXT' | 'IMAGE'>('TEXT')

// 表单状态
const formState = reactive({
  keyword: '',
  tags: '',
  semanticQuery: '',  // 语义描述搜索
  sortBy: 'random',
  sortDirection: 'DESC',
  widthMin: null as number | null,
  widthMax: null as number | null,
  heightMin: null as number | null,
  heightMax: null as number | null,
  sizeMin: null as number | null, // MB
  sizeMax: null as number | null  // MB
})

// 以图搜图状态
const imageSearchState = reactive({
  file: undefined as File | undefined,
  threshold: 0.7
})

// 图搜上传
const fileList = ref<any[]>([])
function handleUploadChange(data: { fileList: any[] }) {
  fileList.value = data.fileList
  if (data.fileList.length > 0) {
    imageSearchState.file = data.fileList[0].file
  } else {
    imageSearchState.file = undefined
  }
}

// 响应式布局控制
const breakpoints = useBreakpoints(breakpointsTailwind)
const isMobile = breakpoints.smaller('lg')
const collapsed = ref(isMobile.value)

watch(isMobile, (val) => {
  collapsed.value = val
})

// 激活的搜索状态（点击搜索时应用）
// 使用 shallowRef 避免深层递归监听 File 对象导致性能问题
const activeSearchState = shallowRef<any>({
  mode: 'TEXT',
  ...formState,
  randomSeed: uuidv4()
})
const page = ref(1)
const pageSize = ref(20)

// 搜索操作
function handleSearch() {
  page.value = 1

  if (searchMode.value === 'TEXT') {
    activeSearchState.value = {
      mode: 'TEXT',
      ...formState,
      randomSeed: uuidv4()
    }
  } else {
    if (!imageSearchState.file) {
      message.warning('请先上传图片')
      return
    }
    activeSearchState.value = {
      mode: 'IMAGE',
      file: imageSearchState.file,
      threshold: imageSearchState.threshold,
      // 这里的 randomSeed 主要用于触发 useQuery 更新
      randomSeed: uuidv4()
    }
  }
}

function handleReset() {
  if (searchMode.value === 'TEXT') {
    formState.keyword = ''
    formState.tags = ''
    formState.semanticQuery = ''
    formState.sortBy = 'random'  // 重置时使用随机排序
    formState.sortDirection = 'DESC'
    formState.widthMin = null
    formState.widthMax = null
    formState.heightMin = null
    formState.heightMax = null
    formState.sizeMin = null
    formState.sizeMax = null

    // 执行随机排序搜索
    page.value = 1
    activeSearchState.value = {
      mode: 'TEXT',
      ...formState,
      randomSeed: uuidv4()
    }

    formState.sortBy = 'similarity'
  } else {
    fileList.value = []
    imageSearchState.file = undefined
    imageSearchState.threshold = 0.7
  }
}

// 查询
const {
  data,
  isLoading,
  refetch,
  isError,
  error
} = useQuery({
  queryKey: ['images', activeSearchState, page, pageSize],
  retry: false,
  refetchOnWindowFocus: false, // 禁止窗口聚焦时自动刷新
  queryFn: async () => {
    const currentState = activeSearchState.value

    // 文本/高级搜索模式
    if (currentState.mode === 'TEXT') {
       const sort = `${currentState.sortBy},${currentState.sortDirection}`

        const startTime = performance.now()
        console.log('[Gallery] 开始搜索图片 (文本模式)...')
        console.log('[Gallery] 搜索参数:', {
          keyword: currentState.keyword,
          tags: currentState.tags,
          semanticQuery: currentState.semanticQuery,
          page: page.value,
          size: pageSize.value,
          sort
        })

        const result = await searchApi.search({
          keyword: currentState.keyword,
          tags: currentState.tags,
          semanticQuery: currentState.semanticQuery || undefined,
          randomSeed: currentState.randomSeed,
          widthMin: currentState.widthMin ?? undefined,
          widthMax: currentState.widthMax ?? undefined,
          heightMin: currentState.heightMin ?? undefined,
          heightMax: currentState.heightMax ?? undefined,
          sizeMin: currentState.sizeMin ? currentState.sizeMin * 1024 * 1024 : undefined,
          sizeMax: currentState.sizeMax ? currentState.sizeMax * 1024 * 1024 : undefined,
          page: page.value - 1,
          size: pageSize.value,
          sort: sort
        })

        const elapsed = (performance.now() - startTime).toFixed(2)
        console.log(`[Gallery] 搜索完成: 找到 ${result.totalElements} 张图片, 耗时 ${elapsed}ms`)

        return result
    }
    // 以图搜图模式
    else {
        if (!currentState.file) return { content: [], totalElements: 0 }

        const startTime = performance.now()
        console.log('[Gallery] 开始搜索图片 (图搜模式)...')

        const result = await searchApi.searchByImage(
            currentState.file,
            currentState.threshold,
            page.value - 1,
            pageSize.value
        )

        const elapsed = (performance.now() - startTime).toFixed(2)
        console.log(`[Gallery] 图搜完成: 找到 ${result.totalElements} 张图片, 耗时 ${elapsed}ms`)

        return result
    }
  }
})

watch(isError, (val) => {
  if (val) {
    const err = error.value as any
    const msg = err?.response?.data?.message || err?.message || '搜索失败'
    message.error(msg)
  }
})

const images = computed(() => data.value?.content || [])
const totalCount = computed(() => data.value?.totalElements || 0)

// 详情弹窗
const showDetail = ref(false)
const selectedDetailImage = ref<ImageDto | null>(null)
const detailLoading = ref(false)

const currentDetailIndex = computed(() => {
  if (!selectedDetailImage.value || images.value.length === 0) return -1
  return images.value.findIndex((img: any) => img.id === selectedDetailImage.value?.id)
})

const hasPrevDetail = computed(() => currentDetailIndex.value > 0)
const hasNextDetail = computed(() => currentDetailIndex.value !== -1 && currentDetailIndex.value < images.value.length - 1)

function handlePrevDetail() {
  if (hasPrevDetail.value) {
    const prev = images.value[currentDetailIndex.value - 1]
    if (prev) {
      detailLoading.value = true
      selectedDetailImage.value = null
      galleryApi.getImage(prev.id).then(fullImage => {
        selectedDetailImage.value = fullImage
      }).catch(err => {
        message.error('获取图片详情失败')
        console.error(err)
      }).finally(() => {
        detailLoading.value = false
      })
    }
  }
}

function handleNextDetail() {
  if (hasNextDetail.value) {
    const next = images.value[currentDetailIndex.value + 1]
    if (next) {
      detailLoading.value = true
      selectedDetailImage.value = null
      galleryApi.getImage(next.id).then(fullImage => {
        selectedDetailImage.value = fullImage
      }).catch(err => {
        message.error('获取图片详情失败')
        console.error(err)
      }).finally(() => {
        detailLoading.value = false
      })
    }
  }
}

function openDetail(image: ImageThumbnailDto) {
  // 先显示弹窗，再异步获取图片详情
  selectedDetailImage.value = null
  detailLoading.value = true
  showDetail.value = true

  galleryApi.getImage(image.id).then(fullImage => {
    selectedDetailImage.value = fullImage
  }).catch(err => {
    message.error('获取图片详情失败')
    console.error(err)
    showDetail.value = false
  }).finally(() => {
    detailLoading.value = false
  })
}

const sortOptions = [
  {label: '匹配度', value: 'similarity'},
  {label: '标题', value: 'title'},
  {label: '随机', value: 'random'},
  {label: '查看次数', value: 'viewCount'},
  {label: '创建时间', value: 'createdAt'},
  {label: '修改时间', value: 'updatedAt'},
  {label: '文件大小', value: 'size'},
]

// 选中状态管理
const selectedIds = ref<Set<number>>(new Set())
const isSelectionMode = computed(() => selectedIds.value.size > 0)

function toggleSelection(id: number) {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
}

function clearSelection() {
  selectedIds.value.clear()
}

const isAllSelected = computed(() => {
  return images.value.length > 0 && images.value.every((img: any) => selectedIds.value.has(img.id))
})

function toggleSelectAll() {
  if (isAllSelected.value) {
    images.value.forEach((img: any) => selectedIds.value.delete(img.id))
  } else {
    images.value.forEach((img: any) => selectedIds.value.add(img.id))
  }
}

// 长按逻辑
const longPressTimer = ref<any>(null)
const longPressTriggered = ref(false)

function handlePointerDown(image: any) {
  longPressTriggered.value = false
  longPressTimer.value = setTimeout(() => {
    longPressTriggered.value = true
    if (!selectedIds.value.has(image.id)) {
      toggleSelection(image.id)
    }
  }, 500)
}

function handlePointerUp() {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

function handleImageClick(image: any) {
  if (longPressTriggered.value) {
    longPressTriggered.value = false
    return
  }

  if (isSelectionMode.value) {
    toggleSelection(image.id)
  } else {
    openDetail(image)
  }
}

// 右键菜单
const showDropdown = ref(false)
const dropdownX = ref(0)
const dropdownY = ref(0)
const currentContextImage = ref<any>(null)

function handleContextMenu(e: MouseEvent, image: any) {
  e.preventDefault()
  showDropdown.value = false
  nextTick().then(() => {
    dropdownX.value = e.clientX
    dropdownY.value = e.clientY
    currentContextImage.value = image
    showDropdown.value = true
  })
}

function handleClickoutside() {
  showDropdown.value = false
}

const dropdownOptions = computed(() => [
  {
    label: '查看详情',
    key: 'view',
    icon: renderIcon(EyeOutline)
  },
  {
    label: selectedIds.value.has(currentContextImage.value?.id) ? '取消选中' : '选中图片',
    key: 'select',
    icon: renderIcon(CheckmarkCircle24Filled)
  },
  {
    label: '下载图片',
    key: 'download',
    icon: renderIcon(DownloadOutline)
  },
  {
    label: '删除图片',
    key: 'delete',
    icon: renderIcon(TrashOutline)
  }
])

function handleSelect(key: string) {
  showDropdown.value = false
  const image = currentContextImage.value
  if (!image) return

  switch (key) {
    case 'view':
      openDetail(image)
      break
    case 'select':
      toggleSelection(image.id)
      break
    case 'download':
      downloadSingleImage(image)
      break
    case 'delete':
      deleteSingleImage(image)
      break
  }
}

function renderIcon(icon: any) {
  return () => h(NIcon, null, {default: () => h(icon)})
}

const message = useMessage()
const dialog = useDialog()

function downloadSingleImage(image: any) {
  const link = document.createElement('a')
  link.href = image.imageUrl
  link.download = image.fileName || (image.title + '.' + image.extension)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function deleteSingleImage(image: any) {
  dialog.warning({
    title: '删除确认',
    content: `确定要删除 "${image.title}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await galleryApi.deleteImage(image.id)
        message.success('删除成功')
        refetch()
        if (selectedIds.value.has(image.id)) {
          selectedIds.value.delete(image.id)
        }
      } catch (e) {
        message.error('删除失败')
      }
    }
  })
}

// 批量操作
async function handleBatchDelete() {
  dialog.warning({
    title: '批量删除确认',
    content: `确定要删除选中的 ${selectedIds.value.size} 张图片吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await galleryApi.deleteImages(Array.from(selectedIds.value))
        message.success('批量删除成功')
        clearSelection()
        refetch()
      } catch (e) {
        message.error('批量删除失败')
      }
    }
  })
}

async function handleBatchDownload() {
  try {
    message.loading('正在打包下载...', {duration: 0})
    const blob = await galleryApi.downloadImages(Array.from(selectedIds.value))
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `images_batch_${new Date().getTime()}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.destroyAll()
    message.success('开始下载')
  } catch (e) {
    message.destroyAll()
    message.error('打包下载失败')
  }
}

// 第一次查询虽然是随机排序，但是查询之后要把排序改成相似度
onMounted(() => {
  // 第一次查询使用的是初始化的 activeSearchState (random)
  // 这里我们将表单显示的默认排序改为相似度，这样用户下一次点击搜索或者看设置时，默认就是相似度
  formState.sortBy = 'similarity';
})

</script>

<template>
  <n-layout has-sider class="h-full">
    <n-layout-sider
        bordered
        collapse-mode="transform"
        :collapsed-width="0"
        :collapsed="collapsed"
        @update:collapsed="(v) => collapsed = v"
        :native-scrollbar="false"
        :class="isMobile ? 'z-50 absolute h-full shadow-xl' : 'z-10'"
        :width="280"
    >
      <div class="flex flex-col h-full">
        <div class="p-4 border-b border-gray-300 dark:border-gray-800">
          <h2 class="text-lg font-medium">搜索</h2>
        </div>

        <div class="flex-1 overflow-y-auto p-4">
          <n-tabs type="segment" animated v-model:value="searchMode">
            <n-tab-pane name="TEXT" tab="高级搜索">
              <n-form label-placement="top" :show-feedback="false" class="mt-4">
                <n-space vertical :size="16">
                  <n-form-item label="语义描述">
                    <n-input
                        v-model:value="formState.semanticQuery"
                        placeholder="描述图片特征..."
                        type="textarea"
                        :autosize="{ minRows: 2, maxRows: 5 }"
                        @keydown.ctrl.enter="handleSearch"
                    />
                  </n-form-item>

                  <n-form-item label="关键字">
                    <n-input
                        v-model:value="formState.keyword"
                        placeholder="标题或文件名"
                        @keydown.enter="handleSearch"/>
                  </n-form-item>

                  <n-form-item label="标签">
                    <tag-search-input
                        v-model:value="formState.tags"
                        placeholder="空格分隔，-排除"
                        :autosize="{ minRows: 2, maxRows: 5 }"
                        @search="handleSearch"
                    />
                  </n-form-item>

                  <div class="grid grid-cols-2 gap-2">
                    <div class="col-span-1">
                      <n-form-item label="宽度 (px)">
                        <div class="flex items-center gap-1 w-full">
                          <n-input-number v-model:value="formState.widthMin" placeholder="MIN" :min="0" class="flex-1" size="small" :show-button="false"/>
                          <span class="text-gray-400">-</span>
                          <n-input-number v-model:value="formState.widthMax" placeholder="MAX" :min="0" class="flex-1" size="small" :show-button="false"/>
                        </div>
                      </n-form-item>
                    </div>

                    <div class="col-span-1">
                      <n-form-item label="高度 (px)">
                        <div class="flex items-center gap-1 w-full">
                          <n-input-number v-model:value="formState.heightMin" placeholder="MIN" :min="0" class="flex-1" size="small" :show-button="false"/>
                          <span class="text-gray-400">-</span>
                          <n-input-number v-model:value="formState.heightMax" placeholder="MAX" :min="0" class="flex-1" size="small" :show-button="false"/>
                        </div>
                      </n-form-item>
                    </div>
                  </div>

                  <n-form-item label="大小 (MB)">
                     <div class="flex items-center gap-2 w-full">
                        <n-input-number v-model:value="formState.sizeMin" placeholder="MIN" :min="0" class="flex-1" size="small" :show-button="false"/>
                        <span class="text-gray-400">-</span>
                        <n-input-number v-model:value="formState.sizeMax" placeholder="MAX" :min="0" class="flex-1" size="small" :show-button="false"/>
                      </div>
                  </n-form-item>

                  <div class="grid grid-cols-2 gap-2">
                    <n-form-item label="排序依据">
                      <n-select v-model:value="formState.sortBy" :options="sortOptions" size="small" />
                    </n-form-item>

                    <n-form-item label="顺序">
                       <n-radio-group v-model:value="formState.sortDirection" name="sortDirection" size="small">
                        <n-space :size="0">
                          <n-radio-button value="ASC">升序</n-radio-button>
                          <n-radio-button value="DESC">降序</n-radio-button>
                        </n-space>
                      </n-radio-group>
                    </n-form-item>
                  </div>
                </n-space>
              </n-form>
            </n-tab-pane>

            <n-tab-pane name="IMAGE" tab="以图搜图" display-directive="show" content-style="height: 100%; display: flex; flex-direction: column;">
               <n-form label-placement="top" :show-feedback="false" class="mt-4 flex flex-col flex-1 h-full">
                <div class="flex flex-col h-full gap-4">
                  <div class="flex-1 min-h-[200px] flex flex-col">
                    <n-form-item label="上传图片" class="flex-1 h-full" content-style="height: 100%;">
                        <n-upload
                            class="w-full h-full flex flex-col"
                            :max="1"
                            directory-dnd
                            accept="image/*"
                            :default-upload="false"
                            :file-list="fileList"
                            @change="handleUploadChange"
                            list-type="image"
                        >
                        <n-upload-dragger class="h-full flex flex-col justify-center items-center bg-gray-50 dark:bg-gray-800 border-dashed border-2 border-gray-300 dark:border-gray-700 rounded-lg hover:border-primary-500 transition-colors">
                            <div class="mb-2">
                            <n-icon size="48" :depth="3">
                                <Archive24Regular />
                            </n-icon>
                            </div>
                            <n-text class="text-base text-gray-500">
                            点击或拖拽上传
                            </n-text>
                        </n-upload-dragger>
                        </n-upload>
                    </n-form-item>
                  </div>

                  <n-form-item label="相似度阈值">
                    <div class="w-full px-2">
                       <n-slider v-model:value="imageSearchState.threshold" :min="0" :max="1" :step="0.01" :format-tooltip="(v) => `${(v * 100).toFixed(0)}%`" />
                       <div class="flex justify-between text-xs text-gray-500 mt-1">
                         <span>0%</span>
                         <span>{{ (imageSearchState.threshold * 100).toFixed(0) }}%</span>
                         <span>100%</span>
                       </div>
                    </div>
                  </n-form-item>
                </div>
              </n-form>
            </n-tab-pane>
          </n-tabs>
        </div>

        <div class="p-4 border-t border-gray-300 dark:border-gray-800 flex gap-2">
          <n-button type="primary" class="flex-1" @click="handleSearch">
            <template #icon>
              <n-icon>
                <Search24Regular/>
              </n-icon>
            </template>
            搜索
          </n-button>
          <n-button class="flex-1" @click="handleReset">
            <template #icon>
              <n-icon>
                <Dismiss24Regular/>
              </n-icon>
            </template>
            重置
          </n-button>
        </div>

      </div>
    </n-layout-sider>

    <div v-if="isMobile && !collapsed" class="absolute inset-0 bg-black/50 z-40" @click="collapsed = true"></div>

    <n-layout class="h-full" content-style="display: flex; flex-direction: column; height: 100%;">

      <!-- 顶部功能栏 -->
      <n-layout-header bordered class="p-3 flex justify-between items-center transition-all"
                       :class="isSelectionMode ? 'bg-primary-50 dark:bg-gray-800' : ''">
        <div v-if="isSelectionMode" class="flex items-center gap-4">
          <n-button circle secondary @click="clearSelection">
            <template #icon>
              <n-icon>
                <CloseCircleOutline/>
              </n-icon>
            </template>
          </n-button>
          <span class="text-base font-medium">已选中 {{ selectedIds.size }} 项</span>
        </div>
        <div v-else class="flex items-center">
          <n-button quaternary circle @click="collapsed = !collapsed">
            <template #icon>
              <n-icon>
                <FilterOutline/>
              </n-icon>
            </template>
          </n-button>
        </div>
        <div class="flex gap-2">
          <n-button secondary @click="toggleSelectAll">
            {{ isAllSelected ? '取消全选' : '全选' }}
          </n-button>
          <n-button type="info" secondary @click="handleBatchDownload" :disabled="!isSelectionMode">
            <template #icon>
              <n-icon>
                <DownloadOutline/>
              </n-icon>
            </template>
            下载
          </n-button>
          <n-button type="error" secondary @click="handleBatchDelete" :disabled="!isSelectionMode">
            <template #icon>
              <n-icon>
                <TrashOutline/>
              </n-icon>
            </template>
            删除
          </n-button>
        </div>
      </n-layout-header>

      <n-layout-content :native-scrollbar="false" content-style="padding: 16px;" class="flex-1">
        <div v-if="isLoading && !images.length" class="flex justify-center items-center h-full">
          <n-spin size="large"/>
        </div>

        <div v-else-if="images.length === 0" class="flex justify-center items-center h-full">
          <n-empty description="未找到图片"/>
        </div>

        <div v-else
             class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
          <div
              v-for="image in images"
              :key="image.id"
              class="relative group cursor-pointer rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow bg-gray-200 dark:bg-gray-800 aspect-square border-2"
              :class="selectedIds.has(image.id) ? 'border-primary-500 ring-2 ring-primary-500/30' : 'border-transparent'"
              @click="handleImageClick(image)"
              @pointerdown="handlePointerDown(image)"
              @pointerup="handlePointerUp"
              @pointerleave="handlePointerUp"
              @contextmenu="handleContextMenu($event, image)"
          >
            <img
                :src="image.thumbnailUrl"
                :alt="image.title || 'image'"
                class="w-full h-full object-cover transition-transform duration-300 transform select-none"
                :class="{ 'scale-90': selectedIds.has(image.id) }"
                loading="lazy"
                draggable="false"
            />

            <!-- 选中遮罩 -->
            <div v-if="selectedIds.has(image.id)" class="absolute inset-0 bg-primary-500/20 pointer-events-none"></div>

            <!-- 选中框 -->
            <div v-if="isSelectionMode" class="absolute top-2 right-2 z-10 transition-all"
                 @click.stop="toggleSelection(image.id)">
              <n-icon size="24"
                      :class="selectedIds.has(image.id) ? 'text-primary-500 bg-white rounded-md' : 'text-white/80 hover:text-white drop-shadow-md'">
                <Checkbox v-if="selectedIds.has(image.id)"/>
                <SquareOutline v-else/>
              </n-icon>
            </div>

            <div
                class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <p class="text-white text-xs truncate">{{ image.title }}</p>
            </div>
          </div>
        </div>
      </n-layout-content>
      <n-layout-footer bordered class="p-4">
        <div class="flex justify-center">
          <n-pagination
              v-model:page="page"
              v-model:page-size="pageSize"
              :item-count="totalCount"
              :page-sizes="[10, 20, 50, 100]"
              :display-order="['size-picker','pages', 'quick-jumper']"
              show-size-picker
              show-quick-jumper
          />
        </div>
      </n-layout-footer>
    </n-layout>

    <ImageDetail
        v-model:show="showDetail"
        v-model:image="selectedDetailImage"
        :loading="detailLoading"
        :has-prev="hasPrevDetail"
        :has-next="hasNextDetail"
        @prev="handlePrevDetail"
        @next="handleNextDetail"
        @refresh="refetch"
    />

    <n-dropdown
        placement="bottom-start"
        trigger="manual"
        :x="dropdownX"
        :y="dropdownY"
        :options="dropdownOptions"
        :show="showDropdown"
        :on-clickoutside="handleClickoutside"
        @select="handleSelect"
    />
  </n-layout>
</template>

