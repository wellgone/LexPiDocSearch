import request from 'umi-request';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatCompletionRequest {
  messages: ChatMessage[];
  model?: string;
  temperature?: number;
  stream?: boolean;
}

export interface ChatCompletionResponse {
  id: string;
  object: string;
  created: number;
  model: string;
  choices: {
    index: number;
    message: ChatMessage;
    finish_reason: string;
  }[];
}

/**
 * 发送聊天请求到 DeepSeek API
 */
export async function chatCompletion(params: ChatCompletionRequest) {
  return request<ChatCompletionResponse>('/ai/completions', {
    method: 'POST',
    data: {
      model: params.model || 'deepseek-chat',
      messages: params.messages,
      temperature: params.temperature || 0.7,
      stream: params.stream || false,
    },
  });
}

/**
 * 发送流式聊天请求到 DeepSeek API
 */
export async function streamChatCompletion(
  params: ChatCompletionRequest,
  onMessage: (text: string) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void,
) {
  try {
    const response = await fetch('/ai/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: params.model || 'deepseek-chat',
        messages: params.messages,
        temperature: params.temperature || 0.7,
        stream: true,
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('Response body is null');
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        onComplete?.();
        break;
      }

      const chunk = decoder.decode(value);
      const lines = chunk
        .split('\n')
        .filter((line) => line.trim() !== '' && line.trim() !== 'data: [DONE]');

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const data = JSON.parse(line.slice(6));
            const text = data.choices[0]?.delta?.content || '';
            if (text) {
              onMessage(text);
            }
          } catch (e) {
            console.error('Error parsing SSE message:', e);
          }
        }
      }
    }
  } catch (error) {
    onError?.(error as Error);
  }
} 