package com.example.logapp.domain.exception

class ActivityHasSessionsException(message: String = "Không thể xoá Activity vì đã có lịch sử session. Hãy dùng tính năng Archive để thay thế.") : Exception(message)
