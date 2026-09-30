import { inject, Service } from '@angular/core';
import { HttpBackend, HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';

@Service()
export class ConfigService {
  private http: HttpClient;
  private appConfig?: { baseUrl: string };

  constructor() {
    this.http = new HttpClient(inject(HttpBackend));
  }

  async init() {
    try {
      this.appConfig = await lastValueFrom(
        this.http.get<{ baseUrl: string }>('assets/config.json'),
      );
      console.log('Конфиг загружен:', this.appConfig);
    } catch (error) {
      console.error('Не удалось загрузить конфиг:', error);
    }
  }

  get baseUrl() {
    if (this.appConfig == null) {
      throw Error('Конфиг не загружен');
    }
    return this.appConfig.baseUrl;
  }
}
