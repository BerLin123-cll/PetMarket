import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 响应式的「当前时间戳」，每秒刷新一次。
 *
 * <p>用于列表里批量渲染多个倒计时：一个定时器驱动全局 now，
 * 各条记录按各自的截止时间与 now 求差值，避免为每行都开一个定时器。</p>
 *
 * @param {number} intervalMs 刷新间隔，默认 1000ms
 * @returns {import('vue').Ref<number>} 每秒递增的时间戳
 */
export function useNow(intervalMs = 1000) {
  const now = ref(Date.now())
  let timer = null

  onMounted(() => {
    timer = setInterval(() => {
      now.value = Date.now()
    }, intervalMs)
  })

  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  })

  return now
}

/**
 * 解析后端返回的时间为时间戳（毫秒）。
 *
 * <p>兼容 "2026-09-16 12:34:56"（后端的 date-format）与 ISO 格式，
 * 无效值返回 null。</p>
 */
export function parseTime(value) {
  if (!value) return null
  if (value instanceof Date) return value.getTime()
  const normalized = String(value).replace(' ', 'T')
  const ts = new Date(normalized).getTime()
  return Number.isNaN(ts) ? null : ts
}

/**
 * 距截止时间还剩多少秒（不足 0 归零）。
 *
 * @param {string|Date} expireTime 截止时间
 * @param {number} nowTs 当前时间戳
 */
export function remainingSeconds(expireTime, nowTs) {
  const end = parseTime(expireTime)
  if (end == null) return 0
  return Math.max(0, Math.floor((end - nowTs) / 1000))
}

/**
 * 把剩余秒数格式化为 mm:ss（超过 1 小时显示 hh:mm:ss）。
 */
export function formatCountdown(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(s / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  const seconds = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  return hours > 0
    ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(minutes)}:${pad(seconds)}`
}
