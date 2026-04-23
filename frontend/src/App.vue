<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'page-static'" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </transition>
  </router-view>
</template>

<style>
.auth-switch-enter-active,
.auth-switch-leave-active {
  transition: opacity 260ms ease, transform 260ms ease;
}

.auth-switch-enter-from {
  opacity: 0;
  transform: translateX(20px) scale(0.985);
}

.auth-switch-leave-to {
  opacity: 0;
  transform: translateX(-20px) scale(0.985);
}
</style>
