package com.ancientbooks.service.impl;

import com.ancientbooks.entity.ChatHistory;
import com.ancientbooks.mapper.ChatHistoryMapper;
import com.ancientbooks.service.ChatHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {
}
