import JSONbig from 'json-bigint';

// 创建一个支持大整数的JSON解析器，将大整数存储为字符串
const JSONBigString = JSONbig({ 
  // 将所有数字都转换为字符串
  storeAsString: true,
  // 总是将数字解析为大整数
  alwaysParseAsBig: false,
  // 使用更安全的数字处理方式
  useNativeBigInt: true
});

/**
 * 解析JSON字符串，自动处理大整数
 * @param text JSON字符串
 * @returns 解析后的对象
 */
export const parseJSON = (text: string): any => {
  if (!text) return text;
  
  try {
    // 尝试使用JSONBigString解析
    const parsed = JSONBigString.parse(text);
    return parsed;
  } catch (err) {
    console.warn('Failed to parse JSON with BigInt support:', err);
    // 如果解析失败，尝试使用原生JSON解析
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
};

/**
 * 将对象转换为JSON字符串，自动处理大整数
 * @param data 要转换的对象
 * @returns JSON字符串
 */
export const stringifyJSON = (data: any): string => {
  if (!data) return '';
  
  try {
    // 尝试使用JSONBigString序列化
    return JSONBigString.stringify(data);
  } catch (err) {
    console.warn('Failed to stringify JSON with BigInt support:', err);
    // 如果转换失败，尝试使用原生JSON转换
    try {
      return JSON.stringify(data, (key, value) => {
        // 对于超大整数，保持其字符串形式
        if (typeof value === 'string' && /^\d{15,}$/.test(value)) {
          return value;
        }
        return value;
      });
    } catch {
      return String(data);
    }
  }
};

/**
 * 判断一个值是否需要作为大整数处理
 * @param value 要判断的值
 * @returns 是否是大整数
 */
export const isBigInt = (value: any): boolean => {
  if (typeof value === 'string') {
    // 检查是否是15位及以上的纯数字字符串
    if (/^\d{15,}$/.test(value)) {
      return true;
    }
    // 检查是否超过安全整数范围
    const num = Number(value);
    return !isNaN(num) && Math.abs(num) > Number.MAX_SAFE_INTEGER;
  }
  return false;
}; 