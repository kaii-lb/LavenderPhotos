package com.kaii.photos.domain.immich

import io.github.kaii_lb.lavender.immichintegration.serialization.user.UserAdminResponseDto

interface ImmichLoginState {
    data class LoggedIn(val user: UserAdminResponseDto) : ImmichLoginState

    object ServerUnreachable : ImmichLoginState

    object LoggedOut : ImmichLoginState
}