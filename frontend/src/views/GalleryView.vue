<script setup lang="ts">
import {useQuery} from '@tanstack/vue-query'
import {searchApi} from '../api/search'
import {galleryApi, type ImageDto} from '../api/gallery'
import {computed, h, nextTick, reactive, ref, watch} from 'vue'
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
  NSpace,
  NSpin,
  useDialog,
  useMessage
} from 'naive-ui'
import ImageDetail from '../components/gallery/ImageDetail.vue'
import TagSearchInput from '../components/gallery/TagSearchInput.vue'
import {breakpointsTailwind, useBreakpoints} from '@vueuse/core'
import {CheckmarkCircle24Filled, Dismiss24Regular, Search24Regular} from '@vicons/fluent'
import {
  Checkbox,
  CloseCircleOutline,
  DownloadOutline,
  EyeOutline,
  FilterOutline,
  FlashOutline,
  SquareOutline,
  TrashOutline
} from '@vicons/ionicons5'
import {v4 as uuidv4} from 'uuid';

// 表单状态
const formState = reactive({
  keyword: '',
  tags: '',
  sortBy: 'RANDOM',
  sortDirection: 'DESC',
  widthMin: null as number | null,
  widthMax: null as number | null,
  heightMin: null as number | null,
  heightMax: null as number | null,
  sizeMin: null as number | null, // MB
  sizeMax: null as number | null  // MB
})

const query = ref('')
const isQueryParsing = ref(false)

async function handleQueryParse() {
  if (!query.value.trim()) return
  try {
    isQueryParsing.value = true
    formState.tags = await searchApi.queryParse(query.value)
    message.success('配置已更新，请点击搜索')
  } catch (e: any) {
    message.error('解析失败: ' + (e.message || '未知错误'))
  } finally {
    isQueryParsing.value = false
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
const activeSearchState = ref({
  ...formState,
  randomSeed: uuidv4()
})
const page = ref(1)
const pageSize = ref(20)

// 搜索操作
function handleSearch() {
  page.value = 1
  activeSearchState.value = {
    ...formState,
    randomSeed: uuidv4()
  }
}

function handleReset() {
  formState.keyword = ''
  formState.tags = ''
  formState.sortBy = 'RANDOM'
  formState.sortDirection = 'DESC'
  formState.widthMin = null
  formState.widthMax = null
  formState.heightMin = null
  formState.heightMax = null
  formState.sizeMin = null
  formState.sizeMax = null
  query.value = ''
  handleSearch()
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
  queryFn: () => {
    const sort = `${activeSearchState.value.sortBy},${activeSearchState.value.sortDirection}`

    return searchApi.search({
      keyword: activeSearchState.value.keyword,
      tags: activeSearchState.value.tags,
      randomSeed: activeSearchState.value.randomSeed,
      widthMin: activeSearchState.value.widthMin ?? undefined,
      widthMax: activeSearchState.value.widthMax ?? undefined,
      heightMin: activeSearchState.value.heightMin ?? undefined,
      heightMax: activeSearchState.value.heightMax ?? undefined,
      sizeMin: activeSearchState.value.sizeMin ? activeSearchState.value.sizeMin * 1024 * 1024 : undefined,
      sizeMax: activeSearchState.value.sizeMax ? activeSearchState.value.sizeMax * 1024 * 1024 : undefined,
      page: page.value - 1,
      size: pageSize.value,
      sort: sort
    })
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
      selectedDetailImage.value = prev
    }
  }
}

function handleNextDetail() {
  if (hasNextDetail.value) {
    const next = images.value[currentDetailIndex.value + 1]
    if (next) {
      selectedDetailImage.value = next
    }
  }
}

function openDetail(image: ImageDto) {
  selectedDetailImage.value = image
  showDetail.value = true
}

const sortOptions = [
  {label: '随机', value: 'RANDOM'},
  {label: '查看次数', value: 'viewCount'},
  {label: '创建时间', value: 'createdAt'},
  {label: '修改时间', value: 'updatedAt'},
  {label: '文件大小', value: 'size'},
  {label: '标题', value: 'title'}
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
        <div class="p-4 border-b border-gray-100 dark:border-gray-800">
          <h2 class="text-lg font-medium">搜索</h2>
        </div>

        <div class="flex-1 overflow-y-auto p-4">
          <div class="mb-5">
            <label class="text-xs font-medium text-gray-500 mb-1 block">智能解析配置</label>
            <n-input
                v-model:value="query"
                placeholder="在此描述想要查找的图片特征（如：高分辨率的风景图），AI 将自动解析并填充下方的搜索表单..."
                type="textarea"
                :autosize="{ minRows: 4, maxRows: 4 }"
                size="small"
                @keydown.ctrl.enter="handleQueryParse"
            />
            <n-button
                type="primary"
                secondary
                block
                dashed
                size="small"
                class="mt-2"
                @click="handleQueryParse"
                :loading="isQueryParsing"
                :disabled="!query"
            >
              <template #icon>
                <n-icon>
                  <FlashOutline/>
                </n-icon>
              </template>
              智能解析配置
            </n-button>
          </div>

          <n-form size="small" label-placement="top" class="pt-2 border-t border-gray-100 dark:border-gray-800">
            <n-form-item label="关键字">
              <n-input
                  v-model:value="formState.keyword"
                  placeholder="标题或文件名"
                  type="textarea"
                  :autosize="{ minRows:1, maxRows:1 }"
                  @keydown.enter="handleSearch"/>
            </n-form-item>

            <n-form-item label="标签搜索">
              <tag-search-input
                  v-model:value="formState.tags"
                  placeholder="输入标签，空格分隔，-排除"
                  :autosize="{minRows:1, maxRows:5}"
                  @search="handleSearch"
              />
            </n-form-item>

            <n-form-item label="宽度范围">
              <div class="flex gap-2 w-full">
                <n-input-number
                    v-model:value="formState.widthMin"
                    placeholder="MIN"
                    :min="0"
                    class="flex-1"
                    size="tiny"
                    :show-button="false"
                />
                <span class="text-gray-400 self-center">-</span>
                <n-input-number
                    v-model:value="formState.widthMax"
                    placeholder="MAX"
                    :min="0"
                    class="flex-1"
                    size="tiny"
                    :show-button="false"
                />
              </div>
            </n-form-item>

            <n-form-item label="高度范围">
              <div class="flex gap-2 w-full">
                <n-input-number
                    v-model:value="formState.heightMin"
                    placeholder="MIN"
                    :min="0"
                    class="flex-1"
                    size="tiny"
                    :show-button="false"
                />
                <span class="text-gray-400 self-center">-</span>
                <n-input-number
                    v-model:value="formState.heightMax"
                    placeholder="MAX"
                    :min="0"
                    class="flex-1"
                    size="tiny"
                    :show-button="false"
                />
              </div>
            </n-form-item>

            <n-form-item label="文件大小 (MB)">
              <div class="flex gap-2 w-full">
                <n-input-number
                    v-model:value="formState.sizeMin"
                    placeholder="MIN"
                    :min="0"
                    class="flex-1"
                    size="tiny"
                    :show-button="false"
                />
                <span class="text-gray-400 self-center">-</span>
                <n-input-number
                    v-model:value="formState.sizeMax"
                    placeholder="MAX"
                    :min="0"
                    class="flex-1"
                    size="tiny"
                    :show-button="false"
                />
              </div>
            </n-form-item>

            <n-form-item label="排序依据">
              <n-select v-model:value="formState.sortBy" :options="sortOptions"/>
            </n-form-item>

            <n-form-item label="排序方向">
              <n-radio-group v-model:value="formState.sortDirection" name="sortDirection">
                <n-space>
                  <n-radio-button value="ASC">升序</n-radio-button>
                  <n-radio-button value="DESC">降序</n-radio-button>
                </n-space>
              </n-radio-group>
            </n-form-item>

          </n-form>
        </div>

        <div class="p-4 border-t border-gray-100 dark:border-gray-800 flex gap-2">
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
                :src="image.thumbnailUrl || image.imageUrl"
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

