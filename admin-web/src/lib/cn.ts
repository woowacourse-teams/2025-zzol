import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * 조건부 클래스를 합치고 충돌은 뒤에 온 것이 이긴다.
 * `cn('p-2', condition && 'p-4')` 처럼 쓰면 p-4 만 남는다.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
