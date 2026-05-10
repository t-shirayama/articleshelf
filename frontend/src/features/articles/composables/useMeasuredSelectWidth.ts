import { computed, nextTick, onMounted, ref, watch, type Ref } from "vue";

export function useMeasuredSelectWidth(label: Ref<string>, cssVariableName: string, extraWidth = 78) {
  const measureEl = ref<HTMLElement | null>(null);
  const width = ref(252);

  const widthStyle = computed(() => ({
    [cssVariableName]: `${width.value}px`,
  }));

  async function updateWidth(): Promise<void> {
    await nextTick();
    if (!measureEl.value) return;
    width.value = Math.ceil(measureEl.value.scrollWidth + extraWidth);
  }

  watch(label, () => {
    void updateWidth();
  });

  onMounted(() => {
    void updateWidth();
  });

  return {
    measureEl,
    widthStyle,
    updateWidth,
  };
}
