import { registerPlugin } from '@capacitor/core';

import type { ApkUpdaterPlugin } from './definitions';

const ApkUpdater = registerPlugin<ApkUpdaterPlugin>('ApkUpdater', {
  web: () => import('./web').then((m) => new m.ApkUpdaterWeb()),
});

export * from './definitions';
export { ApkUpdater };
