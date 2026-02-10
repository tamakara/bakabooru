<script setup lang="ts">
import ImageUploader from '../components/upload/ImageUploader.vue'
import UploadTaskQueue from '../components/upload/UploadTaskQueue.vue'
</script>

<template>
  <div class="h-full flex flex-col gap-2">
    <image-uploader />
    <upload-task-queue />
  </div>
</template>

