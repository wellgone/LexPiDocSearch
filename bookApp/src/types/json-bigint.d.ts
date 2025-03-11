declare module 'json-bigint' {
  interface JSONBigOptions {
    storeAsString?: boolean;
    useNativeBigInt?: boolean;
    alwaysParseAsBig?: boolean;
    protoAction?: 'error' | 'ignore' | 'preserve';
    constructorAction?: 'error' | 'ignore' | 'preserve';
  }

  interface JSONBigInt {
    parse(text: string, reviver?: (key: any, value: any) => any): any;
    stringify(value: any, replacer?: (key: string, value: any) => any, space?: string | number): string;
  }

  function JSONBig(options?: JSONBigOptions): JSONBigInt;
  
  export = JSONBig;
} 