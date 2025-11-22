LetterEvents.registerLetterRules(event => {
    event.createAI('first_gift_kjs', 'lonesome', '主人获得了成就:${str}，请为他写封信')
        .trigger('touhou_little_maid_epistalove:advancement_gain')
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
        .id('first_gift')
        .aiGenerator('', '')
        .trigger('touhou_little_maid_epistalove:first_gift_trigger')
        .repeat()
        .minAffection(30)
        .cooldown(100)
        .register()
})

PlayerEvents.loggedIn(event => {
    const player = event.player
    LetterAPI.triggerEvent(player, 'touhou_little_maid_epistalove:player_join')
})

PlayerEvents.advancement(event => {
    const player = event.player;
    let advancement = event.advancement;
    if (advancement.description.empty) return;
    let str = `${advancement.displayText.getString()}:${advancement.description.getString()}(${advancement.description.getContents().getKey()})`;

    LetterAPI.triggerEventWithContext(player, 'touhou_little_maid_epistalove:advancement_gain', {
        str: str
    })
})