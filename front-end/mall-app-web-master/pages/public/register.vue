<template>
	<view class="container">
		<view class="left-bottom-sign"></view>
		<view class="back-btn yticon icon-zuojiantou-up" @click="navBack"></view>
		<view class="right-top-sign"></view>
		<view class="wrapper">
			<view class="left-top-sign">REGISTER</view>
			<view class="welcome">
				创建一个新账号
			</view>
			<view class="input-content">
				<view class="input-item">
					<text class="tit">用户名</text>
					<input type="text" v-model="username" placeholder="3-50位字母或数字" maxlength="50" />
				</view>
				<view class="input-item">
					<text class="tit">昵称</text>
					<input type="text" v-model="nickname" placeholder="2-80位昵称" maxlength="80" />
				</view>
				<view class="input-item">
					<text class="tit">密码</text>
					<input
						type="text"
						v-model="password"
						placeholder="至少8位"
						placeholder-class="input-empty"
						maxlength="64"
						password
					/>
				</view>
				<view class="input-item">
					<text class="tit">确认密码</text>
					<input
						type="text"
						v-model="confirmPassword"
						placeholder="请再次输入密码"
						placeholder-class="input-empty"
						maxlength="64"
						password
						@confirm="toRegister"
					/>
				</view>
			</view>
			<button class="confirm-btn" @click="toRegister" :disabled="registering">注册并登录</button>
			<view class="register-section">
				已有账号?
				<text @click="toLogin">返回登录</text>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		mapMutations
	} from 'vuex';
	import {
		memberRegister,
		memberLogin,
		memberInfo
	} from '@/api/member.js';

	export default {
		data() {
			return {
				username: '',
				nickname: '',
				password: '',
				confirmPassword: '',
				registering: false
			}
		},
		methods: {
			...mapMutations(['login']),
			navBack() {
				this.navigateAfterAuth();
			},
			toLogin() {
				this.navigateAfterAuth();
			},
			navigateAfterAuth() {
				const pages = getCurrentPages();
				if (pages.length > 1) {
					uni.navigateBack();
					return;
				}

				uni.switchTab({
					url: '/pages/user/user'
				});
			},
			async toRegister() {
				const username = this.username.trim();
				const nickname = this.nickname.trim();
				const password = this.password;
				const confirmPassword = this.confirmPassword;

				if (username.length < 3) {
					uni.showToast({
						title: '用户名至少 3 位',
						icon: 'none'
					});
					return;
				}

				if (nickname.length < 2) {
					uni.showToast({
						title: '昵称至少 2 位',
						icon: 'none'
					});
					return;
				}

				if (password.length < 8) {
					uni.showToast({
						title: '密码至少 8 位',
						icon: 'none'
					});
					return;
				}

				if (password !== confirmPassword) {
					uni.showToast({
						title: '两次输入的密码不一致',
						icon: 'none'
					});
					return;
				}

				this.registering = true;
				try {
					await memberRegister({
						username,
						nickname,
						password
					});

					const loginResponse = await memberLogin({
						username,
						password
					});
					const token = loginResponse.data.tokenHead + loginResponse.data.token;
					uni.setStorageSync('token', token);
					uni.setStorageSync('username', username);
					uni.setStorageSync('password', password);

					const userResponse = await memberInfo();
					this.login(userResponse.data);

					uni.showToast({
						title: '注册成功',
						icon: 'success'
					});
					setTimeout(() => {
						this.registering = false;
						this.navigateAfterAuth();
					}, 400);
				} catch (error) {
					this.registering = false;
					return error;
				}
			}
		},
	}
</script>

<style lang='scss'>
	page {
		background: #fff;
	}

	.container {
		padding-top: 115px;
		position: relative;
		width: 100vw;
		height: 100vh;
		overflow: hidden;
		background: #fff;
	}

	.wrapper {
		position: relative;
		z-index: 90;
		background: #fff;
		padding-bottom: 40upx;
	}

	.back-btn {
		position: absolute;
		left: 40upx;
		z-index: 9999;
		padding-top: var(--status-bar-height);
		top: 40upx;
		font-size: 40upx;
		color: $font-color-dark;
	}

	.left-top-sign {
		font-size: 120upx;
		color: $page-color-base;
		position: relative;
		left: -16upx;
	}

	.welcome {
		position: relative;
		left: 50upx;
		top: -90upx;
		font-size: 46upx;
		color: #555;
		text-shadow: 1px 0px 1px rgba(0, 0, 0, .3);
	}

	.input-content {
		padding: 0 60upx;
	}

	.input-item {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		justify-content: center;
		padding: 0 30upx;
		background: $page-color-light;
		height: 120upx;
		border-radius: 4px;
		margin-bottom: 32upx;

		&:last-child {
			margin-bottom: 0;
		}

		.tit {
			height: 50upx;
			line-height: 56upx;
			font-size: $font-sm+2upx;
			color: $font-color-base;
		}

		input {
			height: 60upx;
			font-size: $font-base + 2upx;
			color: $font-color-dark;
			width: 100%;
		}
	}

	.confirm-btn {
		width: 630upx;
		height: 76upx;
		line-height: 76upx;
		border-radius: 50px;
		margin-top: 70upx;
		background: $uni-color-primary;
		color: #fff;
		font-size: $font-lg;

		&:after {
			border-radius: 100px;
		}
	}

	.register-section {
		margin-top: 30upx;
		width: 100%;
		font-size: $font-sm;
		color: $font-color-base;
		text-align: center;

		text {
			color: $uni-color-primary;
			margin-left: 10upx;
		}
	}

	.right-top-sign {
		position: absolute;
		top: 80upx;
		right: -30upx;
		z-index: 95;

		&:before,
		&:after {
			display: block;
			content: "";
			width: 400upx;
			height: 80upx;
			background: #b4f3e2;
		}

		&:before {
			transform: rotate(50deg);
			border-radius: 0 50px 0 0;
		}

		&:after {
			position: absolute;
			right: -198upx;
			top: 0;
			transform: rotate(-50deg);
			border-radius: 50px 0 0 0;
			/* background: pink; */
		}
	}

	.left-bottom-sign {
		position: absolute;
		left: -270upx;
		bottom: -320upx;
		border: 100upx solid #d0d1fd;
		border-radius: 50%;
		padding: 180upx;
	}
</style>
