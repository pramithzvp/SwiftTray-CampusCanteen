import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({root:'frontend',plugins:[react()],server:{allowedHosts:['terminal.local'],proxy:{'/api':{target:'http://localhost:8080',changeOrigin:true}}},build:{outDir:'../dist',emptyOutDir:true}});
