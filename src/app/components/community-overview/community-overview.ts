import { Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type CommunityOverviewSection =
  | 'posts'
  | 'members'
  | 'productivity'
  | 'events'
  | 'channels'
  | 'moderation';

interface OverviewCard {
  section: CommunityOverviewSection;
  icon: string;
  title: string;
  description: string;
  value?: string;
}

@Component({
  selector: 'app-community-overview',
  imports: [MatIconModule],
  templateUrl: './community-overview.html',
  styleUrl: './community-overview.css'
})
export class CommunityOverview {
  readonly memberCount = input.required<number>();
  readonly postCount = input.required<number>();
  readonly postsLoading = input(false);
  readonly canManage = input(false);
  readonly sectionSelected = output<CommunityOverviewSection>();

  protected readonly cards = computed<OverviewCard[]>(() => {
    const cards: OverviewCard[] = [
      {
        section: 'posts',
        icon: 'dynamic_feed',
        title: 'Publicaciones',
        description: 'Lee novedades y participa en las conversaciones.',
        value: this.postsLoading() ? 'Cargando' : `${this.postCount()} publicadas`
      },
      {
        section: 'members',
        icon: 'groups',
        title: 'Miembros',
        description: 'Conoce a las personas que forman este espacio.',
        value: `${this.memberCount()} integrantes`
      },
      {
        section: 'events',
        icon: 'photo_library',
        title: 'Eventos y galería',
        description: 'Descubre actividades, álbumes y recuerdos del grupo.'
      },
      {
        section: 'productivity',
        icon: 'workspace_premium',
        title: 'Progreso',
        description: 'Consulta tareas, logros, estadísticas y ranking.'
      },
      {
        section: 'channels',
        icon: 'forum',
        title: 'Canales',
        description: 'Encuentra los espacios de comunicación disponibles.'
      }
    ];

    if (this.canManage()) {
      cards.push({
        section: 'moderation',
        icon: 'shield',
        title: 'Moderación',
        description: 'Revisa reportes y protege la convivencia del grupo.'
      });
    }

    return cards;
  });
}
