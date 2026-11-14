import { Component } from '@angular/core';
import { BlockUIModule } from '@openng/optimus-ui/blockui';
import { ToolbarModule } from '@openng/optimus-ui/toolbar';

@Component({
  imports: [BlockUIModule, ToolbarModule],
  selector: 'pro-legacy-tree-table-demo',
  styleUrl: './tree-table-demo.component.css',
  templateUrl: './tree-table-demo.component.html',
})
export class TreeTableDemoComponent {

  constructor() {
    const texts = [
      'Документ',
      'Заявка',
      'Пользователь',
      'Транзакция',
      'Операция',
      'Настройка',
      'Категория',
      'Элемент',
      'Запись',
      'Событие',
      'Проект',
      'Задача',
      'Отчёт',
      'Параметр',
      'Сущность',
      'Ресурс',
    ];

    const randomText = () => texts[Math.floor(Math.random() * texts.length)];

    const randomNumeric = () => Number((Math.random() * 10000).toFixed(2));

    let id = 1;

    const data = Array.from({ length: 20 }, () => {
      const parent: any = {
        id: id++,
        flag: Math.random() > 0.5,
        text: randomText(),
        numeric: randomNumeric(),
        children: [],
      };

      parent.children = Array.from({ length: 4 }, () => ({
        id: id++,
        flag: Math.random() > 0.5,
        text: randomText(),
        numeric: randomNumeric(),
        children: [],
      }));

      return parent;
    });
  }

}
