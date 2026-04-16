const IMAGE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp']

export function isImageFileType(fileType) {
  if (!fileType) return false
  const normalized = String(fileType).toUpperCase()
  return normalized === 'PNG' || normalized === 'JPG' || normalized === 'JPEG'
}

export function isImagePath(path) {
  if (!path) return false
  const lower = String(path).toLowerCase()
  return IMAGE_EXTENSIONS.some((ext) => lower.endsWith(ext))
}

export function toFileUrl(path) {
  if (!path) return ''
  const raw = String(path).replace(/\\/g, '/')
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  if (raw.startsWith('/files/')) return raw
  if (raw.startsWith('ct/') || raw.startsWith('overlay/') || raw.startsWith('result/')) {
    return `/files/${raw}`
  }

  const anchorSegments = ['/ct/', '/overlay/', '/result/']
  for (const segment of anchorSegments) {
    const idx = raw.indexOf(segment)
    if (idx >= 0) {
      return `/files${raw.substring(idx)}`
    }
  }
  return raw
}
