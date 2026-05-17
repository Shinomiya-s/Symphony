<template>
  <div class="canvas-page">
    <!-- 顶部工具栏 -->
    <header class="toolbar">
      <div class="toolbar-left">
        <button class="back-btn" @click="$router.push('/home')">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M10 3L5 8L10 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="brand-divider"></div>
        <span class="brand-name">Symphony</span>
        <span class="brand-sep">·</span>
        <div
            class="board-title"
            contenteditable="true"
            @blur="e => boardTitle = e.target.innerText"
            @keydown.enter.prevent="$event.target.blur()"
        >{{ boardTitle }}</div>
      </div>

      <div class="toolbar-center">
        <div class="tool-group">
          <button
              v-for="tool in tools"
              :key="tool.id"
              :class="['tool-btn', { active: currentTool === tool.id }]"
              @click="selectTool(tool.id)"
              :title="tool.label"
          >
            <component :is="tool.icon" />
          </button>
        </div>

        <div class="tool-sep"></div>

        <div class="tool-group">
          <button class="tool-btn" @click="undo" :disabled="!canUndo" title="撤销 ⌘Z">
            <IconUndo />
          </button>
          <button class="tool-btn" @click="redo" :disabled="!canRedo" title="重做 ⌘⇧Z">
            <IconRedo />
          </button>
          <button class="tool-btn danger" @click="clearCanvas" title="清空画布">
            <IconTrash />
          </button>
        </div>
      </div>

      <div class="toolbar-right">
        <div class="status-dot" :class="{ drawing: currentTool === 'pen' || currentTool === 'eraser' }"></div>
        <span class="status-text">{{ statusText }}</span>
      </div>
    </header>

    <!-- 主体 -->
    <div class="main-layout">
      <!-- 侧边面板 -->
      <aside class="side-panel">
        <section class="panel-section">
          <p class="section-label">颜色</p>
          <div class="color-grid">
            <button
                v-for="color in colors"
                :key="color.value"
                :class="['color-swatch', { active: currentColor === color.value }]"
                :style="{ background: color.value }"
                :title="color.name"
                @click="selectColor(color.value)"
            >
              <span v-if="currentColor === color.value" class="check-mark">✓</span>
            </button>
          </div>
          <!-- 自定义颜色 -->
          <div class="custom-color-row">
            <label class="custom-color-label">
              <input type="color" v-model="currentColor" @input="applyCustomColor" class="color-input-hidden" />
              <span class="custom-color-preview" :style="{ background: currentColor }"></span>
              <span class="custom-color-text">自定义</span>
            </label>
            <span class="hex-value">{{ currentColor.toUpperCase() }}</span>
          </div>
        </section>

        <section class="panel-section">
          <p class="section-label">粗细 <span class="value-badge">{{ brushSize }}px</span></p>
          <div class="slider-track">
            <input
                type="range"
                class="slider"
                v-model.number="brushSize"
                min="1"
                max="30"
            />
            <div class="size-preview" :style="{ width: previewSize + 'px', height: previewSize + 'px', background: currentColor }"></div>
          </div>
        </section>


      </aside>

      <!-- 画布 -->
      <div class="canvas-wrapper" ref="wrapperRef">
        <div class="canvas-inner">
          <canvas ref="canvasRef"></canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, defineComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import * as fabric from 'fabric'

const router = useRouter()
const canvasRef = ref(null)
const wrapperRef = ref(null)
let canvas = null   //Fabric.js 画布实例

const boardTitle = ref('未命名白板')
const currentTool = ref('select')
const currentColor = ref('#1a1a2e')
const brushSize = ref(3)
const history = ref([])
const historyStep = ref(-1)
let isRestoring = false

const canUndo = computed(() => historyStep.value > 0)
const canRedo = computed(() => historyStep.value < history.value.length - 1)
const previewSize = computed(() => Math.min(4 + brushSize.value * 1.2, 28))
const statusText = computed(() => {
  const map = { select: '选择模式', pen: '绘制中', eraser: '橡皮擦', text: '文本模式' }
  return map[currentTool.value] || '就绪'
})

const colors = [
  { value: '#1a1a2e', name: '深夜蓝' },
  { value: '#2d2d2d', name: '炭黑' },
  { value: '#5c5470', name: '暗紫' },
  { value: '#b5838d', name: '玫瑰' },
  { value: '#e8a598', name: '珊瑚' },
  { value: '#c9ada7', name: '藕粉' },
  { value: '#9a8c98', name: '雾紫' },
  { value: '#4a7c59', name: '苔绿' },
  { value: '#6b8cae', name: '雾蓝' },
  { value: '#f2ede4', name: '象牙' },
]

//简易图标组件
const mkIcon = (path) => defineComponent({ render: () => h('svg', { width: 15, height: 15, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.8, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d: path })]) })
const IconSelect = mkIcon('M5 3l14 9-7 1-4 7z')
const IconPen    = mkIcon('M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z')
const IconEraser = mkIcon('M20 20H7L3 16l10-10 7 7-3.5 3.5M6.5 17.5l5-5')
const IconText   = mkIcon('M4 7V4h16v3M9 20h6M12 4v16')
const IconUndo   = mkIcon('M3 7v6h6M3.13 13A9 9 0 1 0 6 5.7L3 7')
const IconRedo   = mkIcon('M21 7v6h-6M20.87 13A9 9 0 1 1 18 5.7L21 7')
const IconTrash  = mkIcon('M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6')

const tools = [
  { id: 'select', label: '选择 (V)', icon: IconSelect },
  { id: 'pen',    label: '画笔 (P)', icon: IconPen },
  { id: 'eraser', label: '橡皮擦 (E)', icon: IconEraser },
  { id: 'text',   label: '文本 (T)', icon: IconText },
]

//保存当前画布快照
const saveState = () => {
  if (isRestoring || !canvas) return
  const json = JSON.stringify(canvas.toJSON())  //把整个画布序列化成JSON
  //和上一步一样就不保存，避免重复
  if (historyStep.value >= 0 && history.value[historyStep.value] === json) return
  //如果在中间步骤操作，清掉后面的历史
  history.value = history.value.slice(0, historyStep.value + 1)
  history.value.push(json)
  historyStep.value++
  //最多保存60步
  if (history.value.length > 60) { history.value.shift(); historyStep.value-- }
}

//撤销
const undo = async () => {
  if (!canUndo.value || isRestoring) return
  isRestoring = true   //加标记，防止loadFromJSON触发saveState
  historyStep.value--  //往前退一步
  await canvas.loadFromJSON(JSON.parse(history.value[historyStep.value]))
  canvas.renderAll()
  isRestoring = false
}

//重做
const redo = async () => {
  if (!canRedo.value || isRestoring) return
  isRestoring = true
  historyStep.value++
  await canvas.loadFromJSON(JSON.parse(history.value[historyStep.value]))
  canvas.renderAll()
  isRestoring = false
}

//工具切换
const selectTool = (toolName) => {
  currentTool.value = toolName
  if (!canvas) return

  //重置状态
  canvas.isDrawingMode = false
  canvas.selection = true
  canvas.defaultCursor = 'default'
  canvas.forEachObject(o => { o.selectable = true; o.evented = true })

  //pen:开启Fabric自带绘制模式
  if (toolName === 'pen') {
    canvas.isDrawingMode = true
    const brush = new fabric.PencilBrush(canvas)
    brush.width = brushSize.value
    brush.color = currentColor.value
    canvas.freeDrawingBrush = brush
    //eraser:不用绘制模式，改用鼠标事件删对象
  } else if (toolName === 'eraser') {
    // 真实对象级擦除：不用 isDrawingMode，用 mouse:move 删对象
    canvas.isDrawingMode = false
    canvas.selection = false
    canvas.defaultCursor = 'cell'
    canvas.forEachObject(o => { o.selectable = false; o.evented = false })
    //text:点击画布创建文本框
  } else if (toolName === 'text') {
    canvas.selection = false
    canvas.defaultCursor = 'text'
    canvas.forEachObject(o => { o.selectable = false; o.evented = false })
  }
}

//橡皮擦:对象级擦除
let isErasing = false

const getPointerXY = (e) => {
  const p = canvas.getScenePoint ? canvas.getScenePoint(e) : canvas.getPointer(e)
  return p
}

const eraseAtPoint = (x, y) => {
  const r = Math.max(brushSize.value, 6)  //擦除半径，最小 6px 保证可用性
  const toRemove = canvas.getObjects().filter(obj => {
    const b = obj.getBoundingRect()
    //圆形擦除区域与对象包围盒相交检测
    const closestX = Math.max(b.left, Math.min(x, b.left + b.width))
    const closestY = Math.max(b.top, Math.min(y, b.top + b.height))
    const dx = x - closestX
    const dy = y - closestY
    return dx * dx + dy * dy <= r * r   //在圆内就删掉
  })
  if (toRemove.length > 0) {
    toRemove.forEach(obj => canvas.remove(obj))
    canvas.renderAll()
    return true  // 有擦除，稍后统一存历史
  }
  return false
}

const handleEraserMouseDown = (opt) => {
  if (currentTool.value !== 'eraser') return
  isErasing = true
  const p = getPointerXY(opt.e)
  eraseAtPoint(p.x, p.y)
}

const handleEraserMouseMove = (opt) => {
  if (currentTool.value !== 'eraser' || !isErasing) return
  const p = getPointerXY(opt.e)
  eraseAtPoint(p.x, p.y)
}

const handleEraserMouseUp = () => {
  if (currentTool.value !== 'eraser') return
  if (isErasing) {
    isErasing = false
    saveState()  //一次拖动结束后统一保存
  }
}

//文本工具
const handleCanvasClick = (opt) => {
  if (currentTool.value !== 'text') return
  if (opt.target) return
  //优先 getScenePoint，回退 getPointer
  const pointer = canvas.getScenePoint
      ? canvas.getScenePoint(opt.e)
      : canvas.getPointer(opt.e)
  //点击画布创建可编辑文本
  const text = new fabric.IText('在此输入', {
    left: pointer.x,
    top: pointer.y,
    fill: currentColor.value,
    fontSize: 20,
    fontFamily: '"Cormorant Garamond", serif',
  })
  canvas.add(text)
  canvas.setActiveObject(text)
  text.enterEditing()   //自动进入编辑状态
  text.selectAll()       //自动全选，方便直接输入
  setTimeout(() => selectTool('select'), 80)    //80ms后切回选择模式
  saveState()
}

const selectColor = (color) => {
  currentColor.value = color
  if (canvas && canvas.freeDrawingBrush) {
    canvas.freeDrawingBrush.color = color
  }
  if (canvas) {
    const obj = canvas.getActiveObject()
    if (obj) { obj.set('fill', color); canvas.renderAll(); saveState() }
  }
}

const applyCustomColor = () => {
  if (canvas && canvas.freeDrawingBrush) canvas.freeDrawingBrush.color = currentColor.value
}

const clearCanvas = () => {
  if (!confirm('确认清空画布？')) return
  canvas.getObjects().slice().forEach(o => canvas.remove(o))
  canvas.renderAll()
  saveState()
}

//自适应画布尺寸
const resizeCanvas = () => {
  if (!canvas || !wrapperRef.value) return
  const w = wrapperRef.value.clientWidth - 48
  const h = wrapperRef.value.clientHeight - 48
  canvas.setWidth(w)
  canvas.setHeight(h)
  canvas.renderAll()
}

//键盘快捷键
const handleKeyDown = (e) => {
  if (e.target.tagName === 'INPUT' || e.target.contentEditable === 'true') return
  if ((e.metaKey || e.ctrlKey) && e.key === 'z') { e.shiftKey ? redo() : undo(); return }
  //字母快捷键切换工具
  const map = { v: 'select', p: 'pen', e: 'eraser', t: 'text' }
  if (map[e.key]) selectTool(map[e.key])
  //Delete/Backspace 删除选中对象
  if ((e.key === 'Delete' || e.key === 'Backspace') && canvas) {
    const obj = canvas.getActiveObject()
    if (obj) { canvas.remove(obj); saveState() }
  }
}

//初始化Fabric画布->绑定鼠标/键盘/窗口事件->保存初始空白快照
onMounted(() => {
  const wEl = wrapperRef.value
  const w = wEl.clientWidth - 48
  const h = wEl.clientHeight - 48

  canvas = new fabric.Canvas(canvasRef.value, {
    backgroundColor: '#f5f0e8',
    enableRetinaScaling: true,
    width: w,
    height: h,
  })

  canvas.on('mouse:down', handleCanvasClick)
  canvas.on('mouse:down', handleEraserMouseDown)
  canvas.on('mouse:move', handleEraserMouseMove)
  canvas.on('mouse:up', handleEraserMouseUp)
  canvas.on('path:created', saveState)
  canvas.on('object:modified', saveState)
  canvas.on('text:editing:exited', saveState)

  saveState()
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('resize', resizeCanvas)
})
//移除键盘/窗口监听->销毁画布释放内存
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('resize', resizeCanvas)
  canvas && canvas.dispose()
})

//实时更新画笔粗细
watch(brushSize, (val) => {
  if (canvas && canvas.freeDrawingBrush && currentTool.value === 'pen') {
    canvas.freeDrawingBrush.width = val
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300&family=Inter:wght@300;400;500&display=swap');

*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

.canvas-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f0e8;
  font-family: 'Inter', sans-serif;
  color: #1a1a2e;
}


.toolbar {
  height: 52px;
  background: #1a1a2e;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 0;
  flex-shrink: 0;
  position: relative;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}

.back-btn {
  width: 30px;
  height: 30px;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 6px;
  color: rgba(255,255,255,0.6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}
.back-btn:hover { border-color: rgba(255,255,255,0.4); color: #fff; }

.brand-divider {
  width: 1px;
  height: 18px;
  background: rgba(255,255,255,0.12);
}

.brand-name {
  font-family: 'Cormorant Garamond', serif;
  font-size: 17px;
  font-weight: 300;
  letter-spacing: 0.06em;
  color: rgba(255,255,255,0.9);
  white-space: nowrap;
}

.brand-sep {
  color: rgba(255,255,255,0.2);
  font-size: 14px;
}

.board-title {
  font-size: 13px;
  font-weight: 400;
  color: rgba(255,255,255,0.45);
  outline: none;
  border-bottom: 1px solid transparent;
  padding: 2px 0;
  transition: all 0.2s;
  min-width: 80px;
  letter-spacing: 0.01em;
}
.board-title:focus {
  color: rgba(255,255,255,0.85);
  border-bottom-color: rgba(255,255,255,0.25);
}

.toolbar-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  padding: 3px;
}

.tool-btn {
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: rgba(255,255,255,0.45);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.tool-btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.9);
}
.tool-btn.active {
  background: rgba(255,255,255,0.12);
  color: #fff;
}
.tool-btn:disabled { opacity: 0.2; cursor: not-allowed; }
.tool-btn.danger:hover:not(:disabled) {
  background: rgba(200,80,80,0.2);
  color: #e87070;
}

.tool-sep {
  width: 1px;
  height: 20px;
  background: rgba(255,255,255,0.1);
  margin: 0 2px;
}

.toolbar-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  transition: background 0.3s;
}
.status-dot.drawing { background: #8bc4a8; box-shadow: 0 0 6px #8bc4a8; }

.status-text {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
  letter-spacing: 0.04em;
}


.main-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}


.side-panel {
  width: 220px;
  background: #faf7f2;
  border-right: 1px solid #e5dfd3;
  padding: 20px 16px;
  overflow-y: auto;
  flex-shrink: 0;
}
.side-panel::-webkit-scrollbar { width: 4px; }
.side-panel::-webkit-scrollbar-thumb { background: #d8d0c4; border-radius: 2px; }

.panel-section {
  margin-bottom: 28px;
}
.panel-section:last-child { margin-bottom: 0; }

.section-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #9a8f83;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.value-badge {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0;
  color: #5c5470;
  text-transform: none;
}


.color-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}

.color-swatch {
  aspect-ratio: 1;
  border-radius: 6px;
  border: 1.5px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.color-swatch:hover { transform: scale(1.1); border-color: rgba(0,0,0,0.15); }
.color-swatch.active { border-color: #1a1a2e; transform: scale(1.05); }

.check-mark {
  font-size: 10px;
  color: rgba(255,255,255,0.9);
  text-shadow: 0 0 4px rgba(0,0,0,0.5);
  pointer-events: none;
}

.custom-color-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.custom-color-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.color-input-hidden {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.custom-color-preview {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid rgba(0,0,0,0.12);
  flex-shrink: 0;
}
.custom-color-text {
  font-size: 11px;
  color: #7a7068;
  letter-spacing: 0.02em;
}
.hex-value {
  font-family: 'Cormorant Garamond', serif;
  font-size: 11px;
  color: #9a8f83;
  letter-spacing: 0.05em;
}


.slider-track {
  display: flex;
  align-items: center;
  gap: 12px;
}
.slider {
  flex: 1;
  height: 4px;
  -webkit-appearance: none;
  background: #e0d8ce;
  border-radius: 2px;
  outline: none;
}
.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1a1a2e;
  cursor: pointer;
  border: 2px solid #f5f0e8;
  box-shadow: 0 1px 4px rgba(0,0,0,0.2);
}

.size-preview {
  border-radius: 50%;
  background: #1a1a2e;
  flex-shrink: 0;
  transition: all 0.15s;
  min-width: 4px;
  min-height: 4px;
}



.canvas-wrapper {
  flex: 1;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #eee9e0;
  background-image:
      radial-gradient(circle at 20% 20%, rgba(181,131,141,0.04) 0%, transparent 50%),
      radial-gradient(circle at 80% 80%, rgba(107,140,174,0.05) 0%, transparent 50%);
}

.canvas-inner {
  box-shadow:
      0 1px 3px rgba(0,0,0,0.06),
      0 8px 32px rgba(0,0,0,0.1),
      0 0 0 1px rgba(0,0,0,0.04);
  border-radius: 3px;
  overflow: hidden;
  line-height: 0;
}
</style>