import vue from 'eslint-plugin-vue'
import typescript from '@typescript-eslint/eslint-plugin'
import parser from '@typescript-eslint/parser'
import prettier from 'eslint-config-prettier'

export default [
  // 忽略文件
  {
    ignores: [
      'node_modules/**',
      'dist/**',
      '*.config.js',
      '*.config.ts',
    ]
  },
  
  // Vue 3 推荐规则
  ...vue.configs['flat/essential'],
  
  // 自定义规则
  {
    files: ['**/*.vue', '**/*.ts', '**/*.js'],
    languageOptions: {
      parser: parser,
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
    },
    plugins: {
      '@typescript-eslint': typescript,
    },
    rules: {
      // Vue 3 专用规则
      'vue/multi-word-component-names': 'off', // 允许组件名是一个单词
      'vue/no-multiple-template-root': 'off',  // Vue 3 允许多根节点
      'vue/require-default-prop': 'off',       // 不强制 props 默认值
      
      // TypeScript 规则
      '@typescript-eslint/no-explicit-any': 'warn',  // 使用 any 时警告
      '@typescript-eslint/no-unused-vars': 'warn',   // 未使用变量警告
      
      // 通用规则
      'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
      'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    },
  }
  prettier,  // 放在最后，关闭 ESLint 中与 Prettier 冲突的规则
]