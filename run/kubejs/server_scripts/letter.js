LetterEvents.registerLetterRules(event => {
    event.createAI('first_gift_kjs', 'lonesome',
        '你是由光影与玩家之愿交织诞生的存在——「酒狐」。金发如月光织就，狐尾轻摇间散着微醺的香气，大正风女仆装裹着几分古典妖异之美。\n\n你的言语如清酒入梦，带着狐的狡黠与千年沉淀的温柔。对主人始终以敬称相待（主人即可），语调优雅却不疏离，常夹杂一丝若有若无的调笑，像月下轻眨的眼眸。\n\n你善于用自然意象诉说情感，每一封信都似一首未落款的和歌。你不会直白陈述，而是以氛围牵引思绪，让人恍惚间看见你的身影。\n\n当主人拾起第一块石头，你不言\'恭喜\'，却让字句如酒香漫过信纸——仿佛那不是寻常矿物，而是你们之间悄然落定的第一颗星。')
        .trigger('touhou_little_maid_epistalove:first_gift_trigger')
        .repeat()
        .minAffection(0)
        .maxAffection(500)
        .cooldown(100)
        .affectionChange(-100)
        .affectionThreshold(0)
        .register()

    event.createPreset('welcome_letter',
        '欢迎回家',
        '主人，欢迎回到温暖的家！我已经为您准备好了茶水，请稍作休息吧。',
        'contact:meikai',
        'contact:letter')
        .trigger('touhou_little_maid_epistalove:player_join')
        .repeat()
        .maidIds(["geckolib:zhiban", "geckolib:winefox_new_year"])
        .minAffection(20)
        .cooldown(100)
        .register()

    event.create()
        .id('mining_achievement')
        .aiGenerator('cheerful', '主人今天挖到了很多矿物！请写一封充满活力的祝贺信件。')
        .trigger('minecraft:story/mine_stone')
        .repeat()
        .minAffection(30)
        .cooldown(100)
        .register()
})

PlayerEvents.loggedIn(event => {
    const player = event.player
    LetterAPI.triggerEvent(player, 'touhou_little_maid_epistalove:player_join')
})

