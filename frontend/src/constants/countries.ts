export interface CountryMeta {
  code: 'CAN' | 'AUS' | 'GBR'
  nameKo: string
  nameEn: string
  flag: string
  gradient: string
  description: string
  descriptionEn: string
  visaNameKo: string
  visaNameEn: string
  lastVerifiedAt: string
}

export const COUNTRIES: CountryMeta[] = [
  {
    code: 'CAN',
    nameKo: '캐나다',
    nameEn: 'CANADA',
    flag: '🇨🇦',
    gradient: 'from-emerald-700 to-slate-800',
    description:
      '높은 삶의 질과 포용적인 이민 정책으로 가장 인기 있는 이주지입니다. EE(Express Entry) 분석을 제공합니다.',
    descriptionEn:
      'The most popular destination thanks to its high quality of life and inclusive immigration policy. Offers Express Entry (EE) analysis.',
    visaNameKo: 'Express Entry (FSW)',
    visaNameEn: 'Express Entry - Federal Skilled Worker',
    lastVerifiedAt: '2026.07.21',
  },
  {
    code: 'AUS',
    nameKo: '호주',
    nameEn: 'AUSTRALIA',
    flag: '🇦🇺',
    gradient: 'from-sky-700 to-orange-500',
    description:
      '따뜻한 기후와 높은 임금 수준이 특징입니다. 기술 이민 비자(Subclass 189) 및 취업 비자 매칭을 지원합니다.',
    descriptionEn:
      'Known for its warm climate and high wages. Supports Skilled Independent visa (Subclass 189) and work visa matching.',
    visaNameKo: 'Skilled Independent Visa (Subclass 189)',
    visaNameEn: 'Skilled Independent Visa',
    lastVerifiedAt: '2026.07.21',
  },
  {
    code: 'GBR',
    nameKo: '영국',
    nameEn: 'UNITED KINGDOM',
    flag: '🇬🇧',
    gradient: 'from-indigo-800 to-slate-700',
    description:
      '유럽 시장의 관문이자 글로벌 기업의 허브입니다. Skilled Worker 비자 및 고학력자 이주 경로 분석을 제공합니다.',
    descriptionEn:
      "A gateway to the European market and a hub for global companies. Offers Skilled Worker visa and highly-educated migration path analysis.",
    visaNameKo: 'Skilled Worker Visa',
    visaNameEn: 'Skilled Worker Visa',
    lastVerifiedAt: '2026.07.21',
  },
]
