export const DEMO_ACTOR_STORAGE_KEY = 'training-platform.demo-actor-id';

export type DemoActorPreset = {
  id: string;
  label: string;
  username: string;
  note: string;
};

export const demoActorPresets: DemoActorPreset[] = [
  {
    id: '13',
    label: 'Андрей Ковалев',
    username: 'NPZ-OP-UKK-001',
    note: 'Оператор UKK: назначенное обучение, попытка, результаты',
  },
  {
    id: '21',
    label: 'Сергей Морозов',
    username: 'NPZ-OP-UPV-001',
    note: 'Оператор UPV: альтернативный assigned-сценарий',
  },
  {
    id: '29',
    label: 'Александр Тарасов',
    username: 'NPZ-OP-MTBEO-001',
    note: 'Самостоятельное обучение и self-testing',
  },
  {
    id: '4',
    label: 'Олег Баранов',
    username: 'NPZ-ITR-CCK-001',
    note: 'Руководитель CCK: supervision и manager read-side',
  },
  {
    id: '11',
    label: 'Марина Лебедева',
    username: 'NPZ-EXP-EDU-001',
    note: 'Эксперт EDU: учебное содержание, вопросы, тесты, final control',
  },
  {
    id: '1',
    label: 'Demo Admin',
    username: 'DEMO-ADMIN-001',
    note: 'Полный административный доступ',
  },
];

function canUseStorage(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

export function getStoredDemoActorId(): string | undefined {
  if (!canUseStorage()) {
    return undefined;
  }

  const storedValue = window.localStorage.getItem(DEMO_ACTOR_STORAGE_KEY)?.trim();
  return storedValue || undefined;
}

export function getEffectiveDemoActorId(): string | undefined {
  const storedValue = getStoredDemoActorId();
  if (!storedValue) {
    return undefined;
  }

  const matchedPreset = demoActorPresets.find((preset) => preset.username === storedValue);
  return matchedPreset?.id ?? storedValue;
}

export function setDemoActorId(actorId?: string): void {
  if (!canUseStorage()) {
    return;
  }

  const normalized = actorId?.trim();
  if (!normalized) {
    window.localStorage.removeItem(DEMO_ACTOR_STORAGE_KEY);
    return;
  }

  window.localStorage.setItem(DEMO_ACTOR_STORAGE_KEY, normalized);
}

export function resetDemoActorId(): void {
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.removeItem(DEMO_ACTOR_STORAGE_KEY);
}

export function resolveDemoActorLabel(username?: string): string | undefined {
  return demoActorPresets.find((preset) => preset.username === username)?.label;
}
