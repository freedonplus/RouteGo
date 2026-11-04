<template>
  <div class="rg-card flex flex-wrap items-end gap-2 px-3 py-2">
    <div class="flex flex-wrap items-end gap-2">
      <slot />
    </div>
    <div class="ml-auto flex shrink-0 items-center gap-1">
      <slot name="actions" />
    </div>
  </div>
</template>
