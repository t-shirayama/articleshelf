<script setup lang="ts">
import DateField from "../../../shared/components/DateField.vue";
import StarRating from "../../../shared/components/StarRating.vue";
import type { ArticleDetailForm } from "../domain/articleForms";
import type { ArticleStatus } from "../types";

defineProps<{
  form: ArticleDetailForm;
  articleRating: number;
  isEditing: boolean;
  submitted: boolean;
  statusOptions: Array<{ label: string; value: Exclude<ArticleStatus, "ALL"> }>;
  readDateError: string;
}>();
</script>

<template>
  <VCard class="detail-meta" variant="flat">
    <VCardText class="detail-meta-content">
      <div class="detail-meta-block">
        <span class="detail-meta-label">{{ $t('common.status') }}</span>
        <VSelect
          v-model="form.status"
          class="articleshelf-select detail-meta-control"
          :items="statusOptions"
          item-title="label"
          item-value="value"
          density="comfortable"
          :disabled="!isEditing"
          hide-details
          variant="outlined"
        />
      </div>

      <div class="detail-meta-block">
        <span class="detail-meta-label">{{ $t('common.readDate') }}</span>
        <DateField
          v-model="form.readDate"
          class="articleshelf-date-field detail-meta-control"
          density="comfortable"
          :disabled="!isEditing"
          :clearable="isEditing"
          :error-messages="submitted && readDateError ? [readDateError] : []"
        />
      </div>

      <div class="detail-meta-block">
        <span class="detail-meta-label">{{ $t('common.rating') }}</span>
        <div class="rating-field detail-rating-field">
          <template v-if="isEditing">
            <StarRating v-model="form.rating" :size="20" />
          </template>
          <template v-else>
            <StarRating :model-value="articleRating" readonly :size="20" />
          </template>
        </div>
      </div>
    </VCardText>
  </VCard>
</template>
