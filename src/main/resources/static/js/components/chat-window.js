const ChatWindow = ({ chat, user, messages, isLoading, hasMore, hasMoreForward, onLoadMore, onLoadMoreForward, onSendMessage, onMarkRead, savedScrollTop, onScrollSave, initialScroll, onInitialScrollDone, onOpenChatInfo, onBack }) => {
    const [newMessageText, setNewMessageText] = React.useState('');
    const [isSending, setIsSending] = React.useState(false);
    const messagesEndRef = React.useRef(null);
    const messagesContainerRef = React.useRef(null);
    const prevChatIdRef = React.useRef(null);
    const prevScrollHeightRef = React.useRef(0);
    const prevMessagesLengthRef = React.useRef(0);
    const isFirstLoadRef = React.useRef(true);
    const scrollContainerRef = messagesContainerRef;
    const SCROLL_THRESHOLD = 100;

    const [showUnreadDivider, setShowUnreadDivider] = React.useState(false);
    const observerRef = React.useRef(null);
    const readTimeoutRef = React.useRef(null);
    const prevUnreadCountRef = React.useRef(0);
    const unreadDividerIndexRef = React.useRef(-1);
    // ✅ Отслеживаем, был ли пользователь внизу
    const wasNearBottomRef = React.useRef(true);

    const lastReadId = React.useMemo(() => {
        const val = chat?.lastReadMessageIdByMe;
        if (val === undefined || val === null) return 0n;
        if (typeof val === 'bigint') return val;
        try { return BigInt(val); } catch { return 0n; }
    }, [chat?.lastReadMessageIdByMe]);

    const isReadByMe = React.useCallback((msg) => {
        if (typeof msg.id === 'string') return true; // temp_ сообщения считаем прочитанными
        const msgId = typeof msg.id === 'bigint' ? msg.id : safeBigInt(msg.id);
        return lastReadId > 0n && msgId <= lastReadId;
    }, [lastReadId]);

    const safeBigInt = (value) => {
        if (value === undefined || value === null) return 0n;
        if (typeof value === 'bigint') return value;
        if (typeof value === 'string' && value.startsWith('temp_')) return 0n;
        try {
            return BigInt(value);
        } catch (e) {
            return 0n;
        }
    };

    // ✅ Проверяем, находится ли пользователь внизу
    const isNearBottom = React.useCallback(() => {
        const container = messagesContainerRef.current;
        if (!container) return true;
        const { scrollTop, scrollHeight, clientHeight } = container;
        return scrollHeight - scrollTop - clientHeight <= SCROLL_THRESHOLD;
    }, []);

    // ===== СМЕНА ЧАТА: либо восстанавливаем сохранённую позицию, либо ждём initialScroll =====
    React.useEffect(() => {
        if (!chat?.id) return;
        if (chat.id === prevChatIdRef.current) return;

        const container = messagesContainerRef.current;

        // Сбрасываем вспомогательные флаги/счётчики для нового чата
        setShowUnreadDivider(false);
        prevScrollHeightRef.current = 0;
        prevMessagesLengthRef.current = 0;
        unreadDividerIndexRef.current = -1;
        prevUnreadCountRef.current = 0;
        wasNearBottomRef.current = true;

        // Есть ли сохранённая позиция для ЭТОГО чата?
        const hasSavedPosition = savedScrollTop !== undefined && savedScrollTop !== null;

        if (hasSavedPosition) {
            // ✅ Повторный вход — восстанавливаем позицию пользователя
            isFirstLoadRef.current = false;
            // Двойной rAF гарантирует, что новые сообщения уже отрисованы в DOM
            requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                if (container) container.scrollTop = savedScrollTop;
            });
            });
        } else {
            // ✅ Первый вход — ждём, пока initialScroll проскроллит к непрочитанным/вниз
            isFirstLoadRef.current = true;
        }

        prevChatIdRef.current = chat.id;
    }, [chat?.id, savedScrollTop]);

    // Observer для отметки прочитанных
    // Observer для отметки прочитанных
    React.useEffect(() => {
        const container = messagesContainerRef.current;
        if (!container) return;

        if (observerRef.current) {
            observerRef.current.disconnect();
            observerRef.current = null;
        }

        // ✅ Отслеживаем ВСЕ непрочитанные (и свои, и чужие)
        const needsReadElements = Array.from(container.querySelectorAll('.needs-read'));
        if (needsReadElements.length === 0) return;

        const observer = new IntersectionObserver((entries) => {
            if (isFirstLoadRef.current) return;

            let maxId = 0n;
            for (const entry of entries) {
                if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                    const msgIdStr = entry.target.getAttribute('data-msg-id');
                    if (!msgIdStr) continue;
                    const msgId = safeBigInt(msgIdStr);
                    if (msgId > 0n && msgId > maxId) maxId = msgId;
                }
            }
            if (maxId > 0n) {
                if (readTimeoutRef.current) clearTimeout(readTimeoutRef.current);
                readTimeoutRef.current = setTimeout(() => {
                    if (maxId > lastReadId) {
                        onMarkRead(chat.id, maxId);
                    }
                }, 1000);
            }
        }, { root: container, threshold: 0.5, rootMargin: '0px' });

        needsReadElements.forEach(el => observer.observe(el));
        observerRef.current = observer;

        return () => {
            if (observerRef.current) observerRef.current.disconnect();
            if (readTimeoutRef.current) clearTimeout(readTimeoutRef.current);
        };
    }, [messages, chat?.id, lastReadId, onMarkRead]);

    // Обработка команды initialScroll (после начальной загрузки без кеша)
    React.useEffect(() => {
    if (!initialScroll || initialScroll.chatId !== chat?.id) return;
    if (messages.length === 0) return;
    const container = messagesContainerRef.current;
    if (!container) return;

    const doScroll = () => {
        // Двойной rAF гарантирует, что DOM уже отрисован
        requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            const hasUnread = messages.some(msg => msg.senderId !== user.id && !isReadByMe(msg));

            if (hasUnread && lastReadId > 0n) {
            setShowUnreadDivider(true);
            const idx = messages.findIndex(msg => msg.senderId !== user.id && !isReadByMe(msg));
            if (idx !== -1) {
                unreadDividerIndexRef.current = idx;
                requestAnimationFrame(() => {
                const dividerEl = container.querySelector('.unread-divider');
                if (dividerEl) {
                    const containerRect = container.getBoundingClientRect();
                    const dividerRect = dividerEl.getBoundingClientRect();
                    const targetScroll = container.scrollTop
                    + (dividerRect.top - containerRect.top)
                    - container.clientHeight
                    + dividerRect.height
                    + 16;
                    container.scrollTo({ top: Math.max(0, targetScroll), behavior: 'auto' });
                } else {
                    container.scrollTop = container.scrollHeight;
                }
                });
            } else {
                container.scrollTop = container.scrollHeight;
            }
            } else {
            setShowUnreadDivider(false);
            unreadDividerIndexRef.current = -1;
            // ✅ Надёжный скролл в самый низ
            container.scrollTop = container.scrollHeight;
            }

            prevScrollHeightRef.current = container.scrollHeight;
            prevMessagesLengthRef.current = messages.length;
            isFirstLoadRef.current = false;
            onInitialScrollDone?.();
        });
        });
    };

    const timer = setTimeout(doScroll, 30);
    return () => clearTimeout(timer);
    }, [initialScroll, chat?.id, messages.length]);

    // Автоматический показ разделителя при появлении новых непрочитанных
    React.useEffect(() => {
        if (!chat || isFirstLoadRef.current) return;
        const currentUnreadCount = messages.filter(msg => msg.senderId !== user.id && !isReadByMe(msg)).length;
        if (currentUnreadCount > 0 && currentUnreadCount > prevUnreadCountRef.current) {
            // ✅ Показываем разделитель ТОЛЬКО если пользователь НЕ внизу
            // Если внизу — сообщение и так видно, разделитель не нужен
            if (!wasNearBottomRef.current) {
                setShowUnreadDivider(true);
                const idx = messages.findIndex(msg => msg.senderId !== user.id && !isReadByMe(msg));
                unreadDividerIndexRef.current = idx;
            }
        }
        prevUnreadCountRef.current = currentUnreadCount;
    }, [messages, chat?.id, lastReadId, user.id]);

    // Обработка добавления сообщений (прокрутка при отправке и подгрузке)
    React.useEffect(() => {
        const container = messagesContainerRef.current;
        if (!container) return;

        const currentLength = messages.length;
        const prevLength = prevMessagesLengthRef.current;

        if (currentLength > prevLength && !isFirstLoadRef.current) {
            const lastMessage = messages[currentLength - 1];
            const addedCount = currentLength - prevLength;

            if (lastMessage && (lastMessage.isPending || String(lastMessage.id).startsWith('temp_'))) {
                // Моё новое сообщение (оптимистичное) → скроллим если был внизу
                if (wasNearBottomRef.current) {
                    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
                }
            } else if (lastMessage && lastMessage.senderId === user.id) {
                // Подтверждение моего сообщения через WS → скроллим если был внизу
                if (wasNearBottomRef.current) {
                    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
                }
            } else if (addedCount <= 2 && lastMessage && lastMessage.senderId !== user.id) {
                // ✅ Чужое новое сообщение (1-2 шт.) → скроллим если пользователь внизу
                if (wasNearBottomRef.current) {
                    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
                }
            } else {
                // Подгрузка старых сообщений (много) → сохраняем позицию
                const newScrollHeight = container.scrollHeight;
                const scrollDelta = newScrollHeight - prevScrollHeightRef.current;
                if (scrollDelta > 0 && container.scrollTop <= SCROLL_THRESHOLD) {
                    container.scrollTop = scrollDelta;
                }
            }
        }

        prevScrollHeightRef.current = container.scrollHeight;
        prevMessagesLengthRef.current = currentLength;
    }, [messages]);

    // Подгрузка истории при скролле
    const handleScroll = React.useCallback(() => {
        const container = scrollContainerRef.current;
        if (!container) return;
        if (isFirstLoadRef.current) return;

        const { scrollTop, scrollHeight, clientHeight } = container;
        wasNearBottomRef.current = (scrollHeight - scrollTop - clientHeight) <= SCROLL_THRESHOLD;

        if (chat?.id && onScrollSave) {
            onScrollSave(chat.id, scrollTop);
        }
        if (!isLoading && hasMore && scrollTop <= SCROLL_THRESHOLD) {
            onLoadMore();
        }
        // Загружаем FORWARD только если есть непрочитанные сообщения
        if (!isLoading && hasMoreForward && chat?.unreadCount > 0 && scrollTop + clientHeight >= scrollHeight - SCROLL_THRESHOLD) {
            onLoadMoreForward();
        }
    }, [isLoading, hasMore, hasMoreForward, onLoadMore, onLoadMoreForward, chat, onScrollSave]);

    React.useEffect(() => {
        const container = messagesContainerRef.current;
        if (container) {
            container.addEventListener('scroll', handleScroll);
            return () => container.removeEventListener('scroll', handleScroll);
        }
    }, [handleScroll]);

    // Обработчик отправки сообщения
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!newMessageText.trim() || isSending) return;

        // ✅ Запоминаем позицию ПЕРЕД отправкой
        wasNearBottomRef.current = isNearBottom();

        setIsSending(true);
        await onSendMessage(chat.id, newMessageText.trim());
        setNewMessageText('');
        // Скрываем разделитель и запоминаем текущее количество непрочитанных
        setShowUnreadDivider(false);
        unreadDividerIndexRef.current = -1;
        prevUnreadCountRef.current = messages.filter(msg => msg.senderId !== user.id && !isReadByMe(msg)).length;
        setIsSending(false);
    };

    // Обработчик добавления сообщений в кеш
    const renderMessagesWithDivider = () => {
        const dividerIndex = showUnreadDivider ? unreadDividerIndexRef.current : -1;
        return messages.map((msg, idx) => {
            const isOwn = msg.senderId === user.id;
            // ✅ Визуальный класс «непрочитанное» — только для чужих (для подсветки)
            const isUnread = !isReadByMe(msg) && !isOwn;
            // ✅ Класс для observer — для ВСЕХ непрочитанных, включая свои
            const needsRead = !isReadByMe(msg) && typeof msg.id !== 'string';
            const showDivider = dividerIndex !== -1 && idx === dividerIndex;

            return (
                <React.Fragment key={msg.id.toString()}>
                    {showDivider && (
                        <div className="unread-divider" key={`divider-${idx}`}>
                            <span>Непрочитанные сообщения</span>
                        </div>
                    )}
                    <div
                        data-msg-id={msg.id.toString()}
                        className={`message-item ${isOwn ? 'my-message' : 'other-message'} ${msg.isPending ? 'pending' : ''} ${isUnread ? 'unread-message' : ''} ${needsRead ? 'needs-read' : ''}`}
                    >
                        {!isOwn && <div className="message-avatar">{getInitials(msg.senderName || 'U')}</div>}
                        <div className="message-bubble">
                            <div className="message-text">{msg.text}</div>
                            <div className="message-meta">
                                <span className="message-time">{formatTime(msg.sentAt)}</span>
                                {isOwn && <span className="message-status">{getMessageStatusIcon(msg)}</span>}
                            </div>
                        </div>
                    </div>
                </React.Fragment>
            );
        });
    };

    const getMessageStatusIcon = (msg) => {
        if (msg.isPending) return <i className="bi bi-clock-history"></i>;
        if (!msg.isDelivered) return <i className="bi bi-check"></i>;
        if (msg.isReadByAnyone) return <i className="bi bi-check2-all"></i>;
        return <i className="bi bi-check"></i>;
    };

    const formatTime = (isoString) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const getInitials = (name) => (name || '').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);

    if (!chat) {
        return (
            <div className="main-area">
                <div className="placeholder-icon"><i className="bi bi-chat-dots"></i></div>
                <div className="placeholder-text">Начните общение!</div>
                <div className="placeholder-subtext">Выберите чат из списка слева</div>
            </div>
        );
    }

    return (
        <div className="chat-window">
            <div className="chat-header">
                <button
                    className="chat-header-btn chat-header-back-btn"
                    onClick={onBack}
                    title="Назад к списку чатов">
                    <i className="bi bi-arrow-left"></i>
                </button>
                <div className="chat-header-avatar">
                    {getInitials(chat.name)}
                </div>
                <div className="chat-header-info">
                    <div className="chat-header-name">{chat.name}</div>
                    <div className="chat-header-status">
                        {chat.chatType === 'PERSONAL'
                            ? 'В сети (заглушка)'
                            : `${chat.membersCount || 0} участников`}
                    </div>
                </div>
                <div className="chat-header-actions">
                    <button className="chat-header-btn" title="Поиск"><i className="bi bi-search"></i></button>
                    {/* ✅ Кнопка "Ещё" открывает информацию о чате */}
                    <button className="chat-header-btn" title="Информация о чате" onClick={() => onOpenChatInfo?.()}>
                        <i className="bi bi-three-dots-vertical"></i>
                    </button>
                </div>
            </div>
            <div className="chat-messages" ref={messagesContainerRef}>
                {isLoading && messages.length === 0 && (
                    <div className="loading-spinner"><i className="bi bi-hourglass-split"></i><p>Загрузка сообщений...</p></div>
                )}
                {renderMessagesWithDivider()}
                <div ref={messagesEndRef} />
            </div>
            <form className="chat-input-form" onSubmit={handleSubmit}>
                <input
                    type="text"
                    className="chat-input"
                    placeholder="Введите сообщение..."
                    value={newMessageText}
                    onChange={(e) => setNewMessageText(e.target.value)}
                    disabled={isSending}
                />
                <button type="submit" className="send-btn" disabled={isSending || !newMessageText.trim()}>
                    <i className="bi bi-send-fill"></i>
                </button>
            </form>
        </div>
    );
};