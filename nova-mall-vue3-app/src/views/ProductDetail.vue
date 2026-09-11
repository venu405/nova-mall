<!--
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本系统已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2020 陈尼克 all rights reserved.
 * 版权所有，侵权必究！
 *
-->

<template>
  <div class="product-detail">
    <s-header :name="'商品详情'"></s-header>
    <div class="detail-content">
      <div class="detail-swipe-wrap">
        <van-swipe class="my-swipe" indicator-color="#1baeae">
          <van-swipe-item v-for="(item, index) in state.detail.goodsCarouselList" :key="index">
            <img :src="item" alt="">
          </van-swipe-item>
        </van-swipe>
      </div>
      <div class="product-info">
        <div class="product-title">
          {{ state.detail.goodsName || '' }}
        </div>
        <div class="product-desc">免邮费 顺丰快递</div>
        <div class="product-price">
          <span>¥{{ state.detail.sellingPrice || '' }}</span>
          <!-- <span>库存203</span> -->
        </div>
      </div>
      <div class="product-intro">
        <ul>
          <li>概述</li>
          <li>参数</li>
          <li>安装服务</li>
          <li>常见问题</li>
        </ul>
        <div class="product-content" v-html="state.detail.goodsDetailContent || ''"></div>
      </div>
    </div>
    <van-action-bar>
      <van-action-bar-icon icon="chat-o" text="客服" />
      <van-action-bar-icon icon="cart-o" :badge="!cart.count ? '' : cart.count" @click="goTo()" text="购物车" />
      <van-action-bar-button type="warning" @click="handleAddCart" text="加入购物车" />
      <van-action-bar-button type="danger" @click="goToCart" text="立即购买" />
    </van-action-bar>
    <!-- AI 商品助手浮动按钮 -->
    <div class="ai-assistant-entry" @click="state.showAiPanel = true">
      <van-icon name="bulb-o" />
      <span>AI 助手</span>
    </div>
    <van-popup
      v-model:show="state.showAiPanel"
      position="bottom"
      round
      :style="{ height: '70%' }"
    >
      <div class="ai-panel">
        <div class="ai-panel-title">AI 商品助手</div>
        <!-- AI 商品简介 -->
        <div class="ai-section">
          <div class="ai-section-header">
            <span class="ai-section-name">AI 商品简介</span>
            <van-button size="small" type="primary" round :loading="state.summaryLoading" @click="handleGenerateSummary">
              {{ state.summary.length ? '重新生成' : '生成简介' }}
            </van-button>
          </div>
          <ul v-if="state.summary.length" class="ai-summary-list">
            <li v-for="(point, index) in state.summary" :key="index">{{ point }}</li>
          </ul>
          <div v-else class="ai-empty">点击"生成简介"，AI 将根据商品信息提炼卖点</div>
        </div>
        <!-- AI 问答 -->
        <div class="ai-section ai-chat-section">
          <div class="ai-section-name">商品问答</div>
          <div class="ai-chat-list">
            <div v-if="!state.chatList.length" class="ai-empty">可以问我关于这个商品的问题，我只根据商品信息回答</div>
            <div v-for="(msg, index) in state.chatList" :key="index" :class="['ai-chat-item', msg.role]">
              <div class="ai-chat-bubble">{{ msg.content }}</div>
            </div>
            <div v-if="state.chatLoading" class="ai-chat-item assistant">
              <div class="ai-chat-bubble">正在思考...</div>
            </div>
          </div>
          <div class="ai-chat-input">
            <van-field
              v-model="state.question"
              placeholder="问一问这个商品"
              maxlength="500"
              @keyup.enter="handleSendQuestion"
            />
            <van-button size="small" type="primary" round :disabled="state.chatLoading" @click="handleSendQuestion">发送</van-button>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { getDetail, getAiSummary, aiChat } from '@/service/good'
import { addCart } from '@/service/cart'
import sHeader from '@/components/SimpleHeader.vue'
import { showSuccessToast, showFailToast } from 'vant'
import { prefix } from '@/common/js/utils'
const route = useRoute()
const router = useRouter()
const cart = useCartStore()

const state = reactive({
  detail: {
    goodsCarouselList: []
  },
  showAiPanel: false,
  summary: [],
  summaryLoading: false,
  question: '',
  chatList: [],
  chatLoading: false
})

onMounted(async () => {
  const { id } = route.params
  const { data } = await getDetail(id)
  data.goodsCarouselList = data.goodsCarouselList.map(i => prefix(i))
  state.detail = data
  cart.updateCart()
})

nextTick(() => {
  // 一些和DOM有关的东西
  const content = document.querySelector('.detail-content')
  content.scrollTop = 0
})

const goBack = () => {
  router.go(-1)
}

const goTo = () => {
  router.push({ path: '/cart' })
}

const handleAddCart = async () => {
  const { resultCode } = await addCart({ goodsCount: 1, goodsId: state.detail.goodsId })
  if (resultCode == 200 ) showSuccessToast('添加成功')
  cart.updateCart()
}

const goToCart = async () => {
  await addCart({ goodsCount: 1, goodsId: state.detail.goodsId })
  cart.updateCart()
  router.push({ path: '/cart' })
}

const handleGenerateSummary = async () => {
  if (state.summaryLoading) return
  state.summaryLoading = true
  try {
    const { data } = await getAiSummary(state.detail.goodsId)
    state.summary = data || []
  } catch (e) {
    showFailToast('AI 简介生成失败，请稍后再试')
  } finally {
    state.summaryLoading = false
  }
}

const handleSendQuestion = async () => {
  const question = (state.question || '').trim()
  if (!question || state.chatLoading) return
  state.chatList.push({ role: 'user', content: question })
  state.question = ''
  state.chatLoading = true
  try {
    const { data } = await aiChat(state.detail.goodsId, question)
    state.chatList.push({ role: 'assistant', content: data })
  } catch (e) {
    state.chatList.push({ role: 'assistant', content: 'AI 助手暂时不可用，请稍后再试。' })
  } finally {
    state.chatLoading = false
  }
}

</script>

<style lang="less">
  @import '../common/style/mixin';
  .product-detail {
    .detail-header {
      position: fixed;
      top: 0;
      left: 0;
      z-index: 10000;
      .fj();
      .wh(100%, 44px);
      line-height: 44px;
      padding: 0 10px;
      .boxSizing();
      color: #252525;
      background: #fff;
      border-bottom: 1px solid #dcdcdc;
      .product-name {
        font-size: 14px;
      }
    }
    .detail-content {
      height: calc(100vh - 50px);
      overflow: hidden;
      overflow-y: auto;
      .detail-swipe-wrap {
        .my-swipe .van-swipe-item {
          img {
            width: 100%;
            // height: 300px;
          }
        }
      }
      .product-info {
        padding: 0 10px;
        .product-title {
          font-size: 18px;
          text-align: left;
          color: #333;
        }
        .product-desc {
          font-size: 14px;
          text-align: left;
          color: #999;
          padding: 5px 0;
        }
        .product-price {
          .fj();
          span:nth-child(1) {
            color: #F63515;
            font-size: 22px;
          }
          span:nth-child(2) {
            color: #999;
            font-size: 16px;
          }
        }
      }
      .product-intro {
        width: 100%;
        padding-bottom: 50px;
        ul {
          .fj();
          width: 100%;
          margin: 10px 0;
          li {
            flex: 1;
            padding: 5px 0;
            text-align: center;
            font-size: 15px;
            border-right: 1px solid #999;
            box-sizing: border-box;
            &:last-child {
              border-right: none;
            }
          }
        }
        .product-content {
          padding: 0 20px;
          img {
            width: 100%;
          }
        }
      }
    }
    .van-action-bar-button--warning {
      background: linear-gradient(to right,#6bd8d8, @primary)
    }
    .van-action-bar-button--danger {
      background: linear-gradient(to right, #0dc3c3, #098888)
    }
    .ai-assistant-entry {
      position: fixed;
      right: 12px;
      bottom: 80px;
      z-index: 100;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: linear-gradient(135deg, #6bd8d8, @primary);
      color: #fff;
      font-size: 12px;
      box-shadow: 0 2px 8px rgba(27, 174, 174, .4);
      .van-icon {
        font-size: 20px;
        margin-bottom: 2px;
      }
    }
    .ai-panel {
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 16px;
      box-sizing: border-box;
      .ai-panel-title {
        font-size: 16px;
        font-weight: bold;
        text-align: center;
        padding-bottom: 10px;
        border-bottom: 1px solid #f0f0f0;
      }
      .ai-section {
        padding: 10px 0;
        .ai-section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .ai-section-name {
          font-size: 14px;
          font-weight: bold;
          color: #333;
        }
        .ai-empty {
          font-size: 12px;
          color: #999;
          padding: 10px 0;
        }
        .ai-summary-list {
          padding: 8px 0 0 16px;
          li {
            font-size: 13px;
            color: #333;
            line-height: 1.8;
            list-style: disc;
          }
        }
      }
      .ai-chat-section {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        border-top: 1px solid #f0f0f0;
        .ai-chat-list {
          flex: 1;
          overflow-y: auto;
          padding: 8px 0;
          .ai-chat-item {
            display: flex;
            margin: 6px 0;
            &.user {
              justify-content: flex-end;
              .ai-chat-bubble {
                background: @primary;
                color: #fff;
              }
            }
            &.assistant .ai-chat-bubble {
              background: #f4f4f4;
              color: #333;
            }
            .ai-chat-bubble {
              max-width: 80%;
              padding: 8px 12px;
              border-radius: 10px;
              font-size: 13px;
              line-height: 1.6;
              word-break: break-all;
            }
          }
        }
        .ai-chat-input {
          display: flex;
          align-items: center;
          gap: 8px;
          .van-field {
            flex: 1;
            background: #f4f4f4;
            border-radius: 16px;
            padding: 4px 12px;
          }
        }
      }
    }
  }
</style>
