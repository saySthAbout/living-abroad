<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'
import EmailVerificationBanner from './EmailVerificationBanner.vue'
</script>

<template>
  <div class="flex min-h-screen flex-col bg-white">
    <div class="print:hidden">
      <AppHeader />
      <EmailVerificationBanner />
    </div>
    <main class="flex-1">
      <slot />
    </main>
    <AppFooter class="print:hidden" />
  </div>
</template>
