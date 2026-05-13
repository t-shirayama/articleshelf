<script setup lang="ts">
import AuthScreen from "../components/AuthScreen.vue";

defineProps<{
  mode: "login" | "register";
}>();
</script>

<template>
  <AuthScreen :mode="mode" />
</template>
