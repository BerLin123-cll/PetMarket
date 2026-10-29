import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        }
    },
    server: {
        port: 5173,
        // 编辑器原子保存时会先生成临时目录，Vite 监听这些目录会因文件锁崩溃
        watch: {
            ignored: ['**/.*.tmpdir/**', '**/*.tmp']
        },
        proxy: {
            '/api': {
                target: 'http://localhost:8081',
                changeOrigin: true
            },
            // 后台接口以 /admin/<模块>/... 形式存在，必须带尾部斜杠，
            // 避免与前端路由 /admin、/admin/login、/admin/product 等冲突。
            '/admin/auth/': { target: 'http://localhost:8081', changeOrigin: true },
            '/admin/product/': { target: 'http://localhost:8081', changeOrigin: true },
            '/admin/category/': { target: 'http://localhost:8081', changeOrigin: true },
            '/admin/user/': { target: 'http://localhost:8081', changeOrigin: true },
            '/admin/order/': { target: 'http://localhost:8081', changeOrigin: true },
            '/admin/dashboard/': { target: 'http://localhost:8081', changeOrigin: true },
            // 本地图片上传接口（保存到后端所在电脑的磁盘）
            '/admin/upload/': { target: 'http://localhost:8081', changeOrigin: true },
            // 本地上传图片的访问路径，代理到后端静态资源映射
            '/uploads/': { target: 'http://localhost:8081', changeOrigin: true }
        }
    }
})
