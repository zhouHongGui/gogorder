// vite.config.ts
import { defineConfig, loadEnv } from "file:///E:/JavaProject/gogorder/gogorder-c/node_modules/vite/dist/node/index.js";
import uniModule from "file:///E:/JavaProject/gogorder/gogorder-c/node_modules/@dcloudio/vite-plugin-uni/dist/index.js";
var uni = uniModule.default || uniModule;
var vite_config_default = defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".");
  return {
    plugins: [uni()],
    server: {
      port: 5173,
      host: true,
      proxy: {
        "/api": {
          target: env.VITE_PROXY_TARGET || "http://localhost:8080",
          changeOrigin: true
        }
      }
    }
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJFOlxcXFxKYXZhUHJvamVjdFxcXFxnb2dvcmRlclxcXFxnb2dvcmRlci1jXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJFOlxcXFxKYXZhUHJvamVjdFxcXFxnb2dvcmRlclxcXFxnb2dvcmRlci1jXFxcXHZpdGUuY29uZmlnLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9FOi9KYXZhUHJvamVjdC9nb2dvcmRlci9nb2dvcmRlci1jL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnLCBsb2FkRW52IH0gZnJvbSAndml0ZSdcbmltcG9ydCB1bmlNb2R1bGUgZnJvbSAnQGRjbG91ZGlvL3ZpdGUtcGx1Z2luLXVuaSdcblxuY29uc3QgdW5pID0gKHVuaU1vZHVsZSBhcyB0eXBlb2YgdW5pTW9kdWxlICYgeyBkZWZhdWx0PzogdHlwZW9mIHVuaU1vZHVsZSB9KS5kZWZhdWx0IHx8IHVuaU1vZHVsZVxuXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoKHsgbW9kZSB9KSA9PiB7XG4gIGNvbnN0IGVudiA9IGxvYWRFbnYobW9kZSwgJy4nKVxuICByZXR1cm4ge1xuICAgIHBsdWdpbnM6IFt1bmkoKV0sXG4gICAgc2VydmVyOiB7XG4gICAgICBwb3J0OiA1MTczLFxuICAgICAgaG9zdDogdHJ1ZSxcbiAgICAgIHByb3h5OiB7XG4gICAgICAgICcvYXBpJzoge1xuICAgICAgICAgIHRhcmdldDogZW52LlZJVEVfUFJPWFlfVEFSR0VUIHx8ICdodHRwOi8vbG9jYWxob3N0OjgwODAnLFxuICAgICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuICB9XG59KVxuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUFnUyxTQUFTLGNBQWMsZUFBZTtBQUN0VSxPQUFPLGVBQWU7QUFFdEIsSUFBTSxNQUFPLFVBQWdFLFdBQVc7QUFFeEYsSUFBTyxzQkFBUSxhQUFhLENBQUMsRUFBRSxLQUFLLE1BQU07QUFDeEMsUUFBTSxNQUFNLFFBQVEsTUFBTSxHQUFHO0FBQzdCLFNBQU87QUFBQSxJQUNMLFNBQVMsQ0FBQyxJQUFJLENBQUM7QUFBQSxJQUNmLFFBQVE7QUFBQSxNQUNOLE1BQU07QUFBQSxNQUNOLE1BQU07QUFBQSxNQUNOLE9BQU87QUFBQSxRQUNMLFFBQVE7QUFBQSxVQUNOLFFBQVEsSUFBSSxxQkFBcUI7QUFBQSxVQUNqQyxjQUFjO0FBQUEsUUFDaEI7QUFBQSxNQUNGO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
