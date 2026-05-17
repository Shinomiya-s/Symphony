<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const svgRef = ref(null)
const wrapperRef = ref(null)
const cursorRef = ref(null)

const mouse = {
  x: 0,
  y: 0,
  smoothX: 0,
  smoothY: 0,
  vx: 0,
  vy: 0,
  smoothVx: 0,
  smoothVy: 0,
  diff: 0
}

const head = {
  x: 0,
  y: 0,
  vx: 0,
  vy: 0
}

const viewport = {
  width: window.innerWidth,
  height: window.innerHeight
}

const particles = []
let particleCnt = 0
let animationId = null

class Particle {
  constructor(x, y, vx, vy, size) {
    this.size = size
    this.x = x
    this.y = y
    this.vx = vx
    this.vy = vy
    this.seed = Math.random() * 1000
    this.freq = (0.5 + Math.random() * 1) * 0.01
    this.amplitude = (1 - Math.random() * 2) * 0.5

    const hue = particleCnt % 360
    const saturation = 100
    const lightness = 50

    this.color = `hsl(${hue}, ${saturation}%, ${lightness}%)`

    this.el = document.createElementNS('http://www.w3.org/2000/svg', 'circle')
    this.el.setAttribute('cx', this.x)
    this.el.setAttribute('cy', this.y)
    this.el.setAttribute('r', this.size)
    this.el.setAttribute('fill', this.color)
  }

  kill() {
    const index = particles.indexOf(this)
    if (index > -1) {
      particles.splice(index, 1)
    }
    this.el.remove()
  }

  render(time) {
    this.x += Math.cos((time + this.seed) * this.freq) * this.amplitude + this.vx
    this.y += Math.sin((time + this.seed) * this.freq) * this.amplitude + this.vy

    this.vx *= 0.95
    this.vy *= 0.95

    this.size += Math.hypot(this.vx, this.vy)
    this.size *= 0.85

    this.el.setAttribute('cy', this.y)
    this.el.setAttribute('cx', this.x)
    this.el.setAttribute('r', this.size)

    const rect = svgRef.value?.getBoundingClientRect()
    if (this.size < 1 ||
        (rect && (this.x < -50 || this.x > rect.width + 50 ||
            this.y < -50 || this.y > rect.height + 50))) {
      this.kill()
    }
  }
}

function onMouseMove(e) {
  if (!svgRef.value) return

  //获取容器的位置
  const rect = svgRef.value.getBoundingClientRect()

  //计算相对于容器的坐标
  const relativeX = e.clientX - rect.left
  const relativeY = e.clientY - rect.top

  //只在容器内部时更新
  if (relativeX >= 0 && relativeX <= rect.width &&
      relativeY >= 0 && relativeY <= rect.height) {
    mouse.vx += mouse.x - relativeX
    mouse.vy += mouse.y - relativeY

    mouse.x = relativeX
    mouse.y = relativeY
  }
}


function onResize() {
  if (!svgRef.value) return

  const rect = svgRef.value.getBoundingClientRect()
  viewport.width = rect.width
  viewport.height = rect.height

  svgRef.value.style.width = rect.width + 'px'
  svgRef.value.style.height = rect.height + 'px'
}


function emitParticle() {
  let x = 0
  let y = 0
  let vx = 0
  let vy = 0
  let size = 0

  if (mouse.diff > 0.01) {
    x = mouse.smoothX
    y = mouse.smoothY
    vx = mouse.smoothVx * -0.25
    vy = mouse.smoothVy * -0.25
    size = mouse.diff * 0.25
  } else {
    x = head.x
    y = head.y
    vx = head.vx * 2
    vy = head.vy * 2
    size = Math.hypot(head.vx, head.vy) * 3
  }

  const particle = new Particle(x, y, vx, vy, size)
  particleCnt += 5

  particles.push(particle)
  if (wrapperRef.value) {
    wrapperRef.value.prepend(particle.el)
  }
}

function render(time) {

  mouse.smoothX += (mouse.x - mouse.smoothX) * 0.1
  mouse.smoothY += (mouse.y - mouse.smoothY) * 0.1

  mouse.smoothVx += (mouse.vx - mouse.smoothVx) * 0.1
  mouse.smoothVy += (mouse.vy - mouse.smoothVy) * 0.1

  mouse.vx *= 0.85
  mouse.vy *= 0.85

  mouse.diff = Math.hypot(mouse.x - mouse.smoothX, mouse.y - mouse.smoothY)

  emitParticle()

  if (cursorRef.value) {
    cursorRef.value.style.transform = `translate3d(${mouse.smoothX}px, ${mouse.smoothY}px, 0)`
  }

  const headX = viewport.width * 0.5 + viewport.width * 0.375 * Math.cos(time * 0.0006)
  const headY = viewport.height * 0.5 + viewport.width * 0.05 * Math.cos(time * 0.0011)

  head.vx = head.x - headX
  head.vy = head.y - headY

  head.x = headX
  head.y = headY

  particles.forEach(particle => {
    particle.render(time)
  })

  animationId = requestAnimationFrame(render)
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('resize', onResize)

  //初始化容器大小
  if (svgRef.value) {
    const rect = svgRef.value.getBoundingClientRect()
    viewport.width = rect.width
    viewport.height = rect.height

    //初始化 head 位置
    head.x = viewport.width * 0.5
    head.y = viewport.height * 0.5
  }

  onResize()
  animationId = requestAnimationFrame(render)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('resize', onResize)

  if (animationId !== null) {
    cancelAnimationFrame(animationId)
  }

  particles.forEach(particle => particle.kill())
  particles.length = 0
})
</script>

<template>
  <div class="rainbow-container">
    <svg
        ref="svgRef"
        xmlns="http://www.w3.org/2000/svg"
        class="rainbow-svg"
        viewBox="0 0 1000 1000"
        preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <mask id="text">
          <text x="50%" y="50%" fill="#fff" dominant-baseline="middle" text-anchor="middle">
            STUDY
          </text>
        </mask>

        <filter id="gooey">
          <feGaussianBlur in="SourceGraphic" stdDeviation="30" />
          <feColorMatrix
              type="matrix"
              values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 18 -7"
              result="goo"
          />
        </filter>
      </defs>

      <text
          class="text"
          x="50%"
          y="50%"
          fill="#fff"
          dominant-baseline="middle"
          text-anchor="middle"
      >
        STUDY
      </text>

      <g ref="wrapperRef" filter="url(#gooey)" mask="url(#text)" class="particle-wrapper"></g>

      <text
          class="overlay"
          x="50%"
          y="50%"
          fill="#fff"
          dominant-baseline="middle"
          text-anchor="middle"
      >
        STUDY
      </text>
    </svg>

    <div ref="cursorRef" class="cursor"></div>
  </div>
</template>

<style scoped>
.rainbow-container {
  width: 100%;
  height: 100%;
  background: #070007;
  cursor: none;
  overflow: hidden;
  position: relative;
}

.rainbow-svg {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
}

.rainbow-svg text {
  font-size: 200px;
  font-family: "DM Sans", sans-serif;
  font-weight: 700;
  line-height: 0.9;
}

.rainbow-svg .text {
  fill: #0f0710;
  stroke: #270a3f;
  stroke-width: 2px;
}

.rainbow-svg .overlay {
  fill: #0f0710;
  mix-blend-mode: overlay;
}

.cursor {
  position: fixed;
  top: -0.5vw;
  left: -0.5vw;
  z-index: 2;
  width: 1vw;
  height: 1vw;
  background: #bbb;
  border-radius: 50%;
  mix-blend-mode: color-dodge;
  pointer-events: none;
}
</style>